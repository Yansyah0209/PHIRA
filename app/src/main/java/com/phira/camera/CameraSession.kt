package com.phira.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.graphics.Rect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.phira.core.*

class CameraSession(private val context: Context) {
    val controller = LifecycleCameraController(context).apply {
        setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
        imageCaptureFlashMode = ImageCapture.FLASH_MODE_OFF
    }
    private val executor = ContextCompat.getMainExecutor(context)
    private val faces = FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).enableTracking().build())
    private val objects = ObjectDetection.getClient(ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.STREAM_MODE).build())
    private var view: PreviewView? = null
    private var owner: LifecycleOwner? = null
    private var active = false
    private var generation = 0
    var subjects by mutableStateOf<List<Subject>>(emptyList()); private set
    var error by mutableStateOf<String?>(null); private set
    var ready by mutableStateOf(false); private set
    var capturing by mutableStateOf(false); private set
    var latest by mutableStateOf<Uri?>(null); private set
    var capturedAt by mutableStateOf(0L); private set
    var lastAnalysisAt by mutableStateOf(0L); private set
    var front by mutableStateOf(false); private set
    var zoom by mutableStateOf(1f); private set
    var flash by mutableStateOf(ImageCapture.FLASH_MODE_OFF); private set

    fun attach(preview: PreviewView, lifecycle: LifecycleOwner) {
        view = preview; owner = lifecycle; active = true
        preview.controller = controller
        controller.zoomState.observe(lifecycle) { state -> zoom = state.zoomRatio }
        bind()
    }
    private fun bind() {
        try { controller.bindToLifecycle(checkNotNull(owner)); ready = true; error = null }
        catch (_: Exception) { ready = false; error = "Camera unavailable. Close other camera apps and retry." }
    }
    fun retry() { if (active) bind() }
    fun analyze(mode: Mode) {
        subjects = emptyList(); lastAnalysisAt = 0
        val token = ++generation
        if (mode == Mode.OBJECT) {
            controller.setImageAnalysisAnalyzer(executor, MlKitAnalyzer(listOf(objects), ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED, executor) { result ->
                if (token != generation || !active) return@MlKitAnalyzer
                if (result.getThrowable(objects) != null) { subjects = emptyList(); error = "Object detection is unavailable. Try Portrait or connect once to download the model." }
                else { subjects = result.getValue(objects).orEmpty().map { normalize(it.boundingBox) }; error = null }
                lastAnalysisAt = SystemClock.elapsedRealtime()
            })
        } else {
            controller.setImageAnalysisAnalyzer(executor, MlKitAnalyzer(listOf(faces), ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED, executor) { result ->
                if (token != generation || !active) return@MlKitAnalyzer
                if (result.getThrowable(faces) != null) { subjects = emptyList(); error = "Face analysis unavailable. You can still take a photo." }
                else { subjects = result.getValue(faces).orEmpty().map { normalize(it.boundingBox) }; error = null }
                lastAnalysisAt = SystemClock.elapsedRealtime()
            })
        }
    }
    private fun normalize(rect: Rect): Subject {
        val width = (view?.width ?: 1).coerceAtLeast(1).toFloat()
        val height = (view?.height ?: 1).coerceAtLeast(1).toFloat()
        return Subject(rect.left / width, rect.top / height, rect.right / width, rect.bottom / height)
    }
    fun flip() {
        if (!ready || capturing) return
        val next = if (front) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
        try {
            if (!controller.hasCamera(next)) { error = "This device has no camera on that side."; return }
            controller.cameraSelector = next; front = !front; subjects = emptyList(); lastAnalysisAt = 0; zoom = 1f
        } catch (_: Exception) { error = "Could not switch cameras." }
    }
    fun setZoom(value: Float) {
        val state = controller.zoomState.value ?: return
        val next = value.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        controller.setZoomRatio(next).addListener({ zoom = controller.zoomState.value?.zoomRatio ?: zoom }, executor)
    }
    fun cycleFlash() {
        flash = when (flash) { ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO; ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON; else -> ImageCapture.FLASH_MODE_OFF }
        controller.imageCaptureFlashMode = flash
    }
    fun capture() {
        if (!ready || capturing || !active) return
        capturing = true
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "PHIRA_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PHIRA")
        }
        val output = ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()
        try {
            controller.takePicture(output, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    capturing = false; latest = result.savedUri; capturedAt = SystemClock.elapsedRealtime(); error = null
                }
                override fun onError(exception: ImageCaptureException) { capturing = false; error = "Photo could not be saved. Check available storage and retry." }
            })
        } catch (_: Exception) { capturing = false; error = "Camera is not ready. Please retry." }
    }
    fun dispose() {
        active = false; generation++; controller.clearImageAnalysisAnalyzer(); controller.unbind()
        owner?.let { controller.zoomState.removeObservers(it) }
        faces.close(); objects.close(); ready = false
    }
}
