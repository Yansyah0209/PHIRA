package com.phira.camera

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
import com.phira.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun PhotoReview(context: Context, uri: Uri, grid: Grid, onClose: () -> Unit) {
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var guidance by remember(uri) { mutableStateOf<Guidance?>(null) }
    var message by remember(uri) { mutableStateOf("Analyzing photo…") }
    LaunchedEffect(uri) {
        try {
            val image = withContext(Dispatchers.IO) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                    val scale = (1600f / maxOf(info.size.width, info.size.height)).coerceAtMost(1f)
                    decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }
            bitmap = image
            val detector = FaceDetection.getClient(FaceDetectorOptions.Builder().build())
            try {
                val detections: List<Subject>? = suspendCoroutine { continuation ->
                    detector.process(InputImage.fromBitmap(image, 0)).addOnSuccessListener { faces ->
                        continuation.resume(faces.map { face -> face.boundingBox.let { Subject(it.left.toFloat() / image.width, it.top.toFloat() / image.height, it.right.toFloat() / image.width, it.bottom.toFloat() / image.height) } })
                    }.addOnFailureListener { continuation.resume(null) }
                }
                if (detections == null) message = "Photo analysis unavailable. Try a different photo."
                else if (detections.isEmpty()) message = "No face detected. Photo review currently supports portraits."
                else guidance = CompositionEngine().evaluate(detections, grid, Mode.PORTRAIT)
            } finally { detector.close() }
        } catch (_: Exception) { message = "Could not open this photo. Please select another image." }
    }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Photo review", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onClose) { Text("Done") }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            bitmap?.let { Image(it.asImageBitmap(), "Selected photograph", Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        }
        Text(guidance?.let { "${it.score ?: "—"} / 100 · Framing alignment" } ?: message, style = MaterialTheme.typography.titleMedium)
        guidance?.let { Text(it.title); Text(it.detail) }
        Text("This measures face placement against your chosen grid. It does not judge artistic quality.", style = MaterialTheme.typography.bodySmall)
    }
}
