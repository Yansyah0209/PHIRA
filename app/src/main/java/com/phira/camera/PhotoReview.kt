package com.phira.camera

import android.content.Intent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.phira.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun PhotoReview(context: Context, uri: Uri, grid: Grid, onClose: () -> Unit) {
    var mode by remember(uri) { mutableStateOf(Mode.PORTRAIT) }
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var guidance by remember(uri) { mutableStateOf<Guidance?>(null) }
    var message by remember(uri) { mutableStateOf("Analyzing photo…") }
    LaunchedEffect(uri, grid, mode) {
        guidance = null
        message = "Analyzing photo…"
        try {
            val image = withContext(Dispatchers.IO) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                    val scale = (1600f / maxOf(info.size.width, info.size.height)).coerceAtMost(1f)
                    decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }
            bitmap = image
            val detector = if (mode == Mode.OBJECT) null else FaceDetection.getClient(FaceDetectorOptions.Builder().build())
            val objectDetector = if (mode == Mode.OBJECT) ObjectDetection.getClient(ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE).enableMultipleObjects().build()) else null
            try {
                fun subject(rect: android.graphics.Rect) = Subject(rect.left.toFloat() / image.width, rect.top.toFloat() / image.height, rect.right.toFloat() / image.width, rect.bottom.toFloat() / image.height)
                val detections: List<Subject>? = suspendCoroutine { continuation ->
                    if (objectDetector != null) {
                        objectDetector.process(InputImage.fromBitmap(image, 0))
                            .addOnSuccessListener { objects -> continuation.resume(objects.map { subject(it.boundingBox) }) }
                            .addOnFailureListener { continuation.resume(null) }
                    } else {
                        checkNotNull(detector).process(InputImage.fromBitmap(image, 0))
                            .addOnSuccessListener { faces -> continuation.resume(faces.map { subject(it.boundingBox) }) }
                            .addOnFailureListener { continuation.resume(null) }
                    }
                }
                if (detections == null) message = "Photo analysis unavailable. Object mode may need an initial model download."
                else if (detections.isEmpty()) message = "No subject detected. Try another mode or photo."
                else guidance = CompositionEngine().evaluate(detections, grid, mode)
            } finally { detector?.close(); objectDetector?.close() }
        } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { message = "Could not open this photo. Please select another image." }
    }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Photo review", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onClose) { Text("Done") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Mode.entries.forEach { option -> FilterChip(selected = mode == option, onClick = { mode = option }, label = { Text(option.label) }) }
        }
        OutlinedButton(onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newUri(context.contentResolver, "PHIRA photo", uri)
            }
            try { context.startActivity(Intent.createChooser(send, "Share photograph")) }
            catch (_: android.content.ActivityNotFoundException) { message = "No sharing app available." }
        }) { Text("Share photo") }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            bitmap?.let { Image(it.asImageBitmap(), "Selected photograph", Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        }
        Text(guidance?.let { "${it.score ?: "—"} / 100 · Framing alignment" } ?: message, style = MaterialTheme.typography.titleMedium)
        guidance?.let { Text(it.title); Text(it.detail) }
        Text("This measures detected subject placement against your chosen grid. It does not judge artistic quality.", style = MaterialTheme.typography.bodySmall)
    }
}
