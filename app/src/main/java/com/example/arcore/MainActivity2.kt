package com.example.arcore

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState.PAUSED
import com.google.ar.core.TrackingState.STOPPED
import com.google.ar.core.TrackingState.TRACKING
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import java.io.IOException

class MainActivity2 : AppCompatActivity() {
    private val TAG = "TAG2"
    private var mSession: Session? = null
    private lateinit var arFragment: ArFragment
    private lateinit var arSceneView: ArSceneView
    private lateinit var andyRenderable: ModelRenderable
    private var modelAdded = false // add model once
    private var sessionConfigured = false
    private val imageFileNames = listOf("car.jpeg", "car2.jpg") // Add more file names as needed


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
        checkSessions()
        arFragment.planeDiscoveryController.hide()
        arFragment.planeDiscoveryController.setInstructionView(null)
        arFragment.arSceneView.scene.addOnUpdateListener { onUpdateFrame(it) }
    }

    private fun checkSessions() {
        Log.d("TAG", "IO Exception while checkSessions")
        if (mSession == null) {
            var message: String? = null
            var exception: Exception? = null
            try {
                mSession = Session(this)
            } catch (e: UnavailableArcoreNotInstalledException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableApkTooOldException) {
                message = "Please update ARCore"
                exception = e
            } catch (e: UnavailableSdkTooOldException) {
                message = "Please update android"
                exception = e
            } catch (e: Exception) {
                message = "AR is not supported"
                exception = e
            }
            if (

                message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Exception creating session", exception)
                return
            }
            sessionConfigured = true
        }
        if (sessionConfigured) {
            configureSession()
            sessionConfigured = false
            arSceneView = arFragment.arSceneView
            arSceneView.setupSession(mSession)
        }
    }

    private fun loadAugmentedImages(): List<Pair<String, Bitmap?>> {
        return try {
            Log.d("TAG", "IO Exception while loadAugmentedImages")

            imageFileNames.map { filename ->
                try {
                    assets.open(filename).use { stream ->
                        filename to BitmapFactory.decodeStream(stream)
                    }
                } catch (e: IOException) {
                    Log.e("ImageLoad", "IO Exception while loading $filename", e)
                    filename to null
                }
            }
        } catch (e: IOException) {
            Log.e("ImageLoad", "IO Exception while loading", e)
            emptyList()
        }
    }

    private fun setupAugmentedImageDb(config: Config): Boolean {
        val augmentedImages = loadAugmentedImages()

        if (augmentedImages.isEmpty()) {
            return false
        }

        val augmentedImageDatabase = AugmentedImageDatabase(mSession)

        for ((imageName, bitmap) in augmentedImages) {
            if (bitmap != null) {
                augmentedImageDatabase.addImage(imageName, bitmap)
            }
        }

        config.setAugmentedImageDatabase(augmentedImageDatabase)
        return true
    }
    private fun onUpdateFrame(frameTime: FrameTime) {
        try {
            val frame: Frame = arFragment.arSceneView.arFrame!!
            val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
            for (augmentedImage in augmentedImages) {
                when (augmentedImage.trackingState) {
                    PAUSED -> {
                        Log.d("TAG", "Detected Image PAUSED: ${augmentedImage.name}")
                        // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                        // but not yet tracked.
                    }

                    TRACKING -> {
                        Log.d("TAG", "Detected Image TRACKING: ${augmentedImage.name}")
                        if (imageFileNames.any { augmentedImage.name.contains(it) } && !modelAdded) {
                            findViewById<ImageView>(R.id.image_view_fit_to_scan)?.visibility = View.GONE
                            renderObject(
                                arFragment,
                                augmentedImage.createAnchor(augmentedImage.centerPose),
                                R.raw.cube
                            )
                            modelAdded = true
                        }
                    }

                    STOPPED -> {
                        Log.d("TAG", "Detected Image STOPPED")
                        augmentedImage.name.contains("car2.jpg")
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("onUpdateFrame", "IO Exception while loading", e)
        }
    }



    private fun renderObject(fragment: ArFragment, anchor: Anchor, model: Int) {
        try {
            ModelRenderable.builder()
                .setSource(this, model)
                .build()
                .thenAccept { modelRenderable -> addNode(anchor, modelRenderable) }
                .exceptionally {
                    Log.e("TAG", "Error building ModelRenderable", it)
                    showErrorDialog(it.localizedMessage)
                    null
                }
        } catch (e: Exception) {
            Log.e("TAG", "Exception in renderObject", e)
            // Handle any specific exception or add additional logging
        }
    }

    private fun addNode(anchor: Anchor, model: ModelRenderable) {
        val anchorNode = AnchorNode(anchor)
        TransformableNode(arFragment.transformationSystem).apply {
            renderable = model
            setParent(anchorNode)
            arFragment.arSceneView.scene.addChild(anchorNode)
            select()
        }
    }

    private fun showErrorDialog(errorMessage: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(errorMessage ?: "Unknown error")
            .setTitle("Error!")
        val dialog = builder.create()
        dialog.show()
    }

    private fun addNodeToScene(fragment: ArFragment, anchor: Anchor, renderable: Renderable) {
        Log.d("TAG", "IO Exception while addNodeToScene")
        val anchorNode = AnchorNode(anchor)
        val node = TransformableNode(fragment.transformationSystem)
        node.renderable = renderable
        node.setParent(anchorNode)
        fragment.arSceneView.scene.addChild(anchorNode)
        node.select()
    }

    override fun onPause() {
        super.onPause()
        Log.d("TAG", "IO Exception while onPause")
        if (mSession != null) {
            arSceneView.pause()
            mSession?.pause()
        }
    }

    private fun configureSession() {
        Log.d("TAG", "IO Exception while configureSession")
        val config = Config(mSession)
        if (!setupAugmentedImageDb(config)) {
            Toast.makeText(this, "Unable to setup augmented", Toast.LENGTH_SHORT).show()
        }
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE)
        mSession?.configure(config)
    }
}