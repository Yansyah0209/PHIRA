package com.phira.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.phira.core.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private val Paper = Color(0xFFFAF9F6)
private val Ink = Color(0xFF252522)
private val Muted = Color(0xFF777770)
private val Line = Color(0xFFE3E2DC)
private val Accent = Color(0xFF648273)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Ink, background = Paper, surface = Paper, onSurface = Ink, secondary = Accent, secondaryContainer = Color(0xFFE8EEE8), onSecondaryContainer = Ink, surfaceVariant = Line, onSurfaceVariant = Muted, outline = Muted, primaryContainer = Line, onPrimaryContainer = Ink)) {
                Surface(Modifier.fillMaxSize(), color = Paper) { PhiraApp() }
            }
        }
    }
}

@Composable
private fun PhiraApp() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var requested by remember { mutableStateOf(false) }
    var review by remember { mutableStateOf<Uri?>(null) }
    BackHandler(enabled = review != null) { review = null }
    val prefs = remember { context.getSharedPreferences("phira", 0) }
    var grid by remember { mutableStateOf(Grid.entries.firstOrNull { it.name == prefs.getString("grid", "PHI") } ?: Grid.PHI) }
    var auto by remember { mutableStateOf(prefs.getBoolean("auto", false)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; requested = true }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) review = it }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(grid, auto) { prefs.edit().putString("grid", grid.name).putBoolean("auto", auto).apply() }
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        when {
            review != null -> PhotoReview(context, review!!, grid) { review = null }
            granted -> CameraScreen(grid, { grid = it }, auto, { auto = it }, { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, { review = it })
            else -> Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
                Image(painterResource(R.drawable.ic_phira), "PHIRA logo", Modifier.size(64.dp))
                Spacer(Modifier.height(28.dp))
                Text("A better frame.\nYour own eye.", fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(16.dp))
                Text("A little guidance, right when you need it. Allow camera access to compose and capture. Portrait analysis runs on your device.", color = Muted)
                Spacer(Modifier.height(28.dp))
                Button(onClick = { permission.launch(Manifest.permission.CAMERA) }, shape = RoundedCornerShape(8.dp)) { Text("Enable camera") }
                if (requested) TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Open app settings") }
                TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Review a portrait instead") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraScreen(grid: Grid, onGrid: (Grid) -> Unit, auto: Boolean, onAuto: (Boolean) -> Unit, onPick: () -> Unit, onReview: (Uri) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val session = remember { CameraSession(context) }
    var mode by remember { mutableStateOf(Mode.PORTRAIT) }
    var roll by remember { mutableStateOf<Float?>(null) }
    var settings by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    val gate = remember { CaptureGate() }
    val engine = remember { CompositionEngine() }
    DisposableEffect(session, owner) {
        val level = LevelSensor(context) { roll = it }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> level.start()
                Lifecycle.Event.ON_PAUSE -> { level.stop(); gate.reset() }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) level.start()
        onDispose { level.stop(); owner.lifecycle.removeObserver(observer); gate.reset(); session.dispose() }
    }
    LaunchedEffect(mode, session.front, grid, auto) { gate.reset(); session.analyze(mode) }
    LaunchedEffect(owner) { owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { while (true) { tick = SystemClock.elapsedRealtime(); delay(100) } } }
    val fresh = tick - session.lastAnalysisAt < 800 && session.lastAnalysisAt > 0
    val subjects = if (fresh) session.subjects else emptyList()
    val guidance = engine.evaluate(subjects, grid, mode, roll)
    val warning = session.error ?: session.analysisError
    LaunchedEffect(tick, auto, guidance.ready, session.capturing, settings) {
        val eligible = auto && !settings && !session.capturing && tick - session.capturedAt > 4000 && guidance.ready && session.ready && warning == null && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        val triggered = gate.update(eligible, tick)
        if (triggered && !session.capturing && tick - session.capturedAt > 4000) session.capture()
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_phira), "PHIRA logo", Modifier.size(38.dp))
            Text("PHIRA", Modifier.padding(start = 8.dp).weight(1f), fontSize = 19.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Tune, "Camera settings") }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            val width = minOf(maxWidth, maxHeight * .75f)
            Box(Modifier.width(width).aspectRatio(.75f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF191A18))) {
                AndroidView(factory = { ctx -> PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE; scaleType = PreviewView.ScaleType.FILL_CENTER; session.attach(this, owner) } }, modifier = Modifier.fillMaxSize())
                CompositionOverlay(grid, subjects, guidance)
                Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    ViewfinderPill("${grid.label.uppercase()} GRID")
                    ViewfinderPill(if (!session.ready) "CONNECTING" else if (auto) "AUTO" else "ON DEVICE")
                }
                Row(Modifier.align(Alignment.TopCenter).padding(top = 40.dp).clip(CircleShape).background(Color.Black.copy(alpha = .4f)), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { session.changeZoom(session.zoom - .5f) }) { Text("−", color = Color.White) }
                    Text("${"%.1f".format(session.zoom)}×", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    TextButton(onClick = { session.changeZoom(session.zoom + .5f) }) { Text("+", color = Color.White) }
                }
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = .62f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (guidance.ready) Icons.Outlined.CheckCircle else Icons.Outlined.MyLocation, null, Modifier.size(16.dp), tint = if (guidance.ready) Color(0xFFBED8C6) else Color.White)
                        Text(warning ?: guidance.title, Modifier.padding(start = 6.dp).weight(1f), color = Color.White, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, maxLines = 3)
                        guidance.score?.let { Text("$it", Modifier.padding(start = 6.dp), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 18.sp, lineHeight = 22.sp) }
                    }
                    Text(if (warning != null) "Manual capture is available when the camera is ready." else guidance.detail, color = Color.White.copy(alpha = .8f), fontSize = 10.sp, lineHeight = 14.sp)
                    if (session.error != null) TextButton(onClick = session::retry, contentPadding = PaddingValues(0.dp)) { Text("Retry", color = Color.White) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("FRAMING ALIGNMENT", color = Color.White.copy(alpha = .6f), fontSize = 7.sp, lineHeight = 10.sp, letterSpacing = 1.sp)
                        Text(roll?.let { "${abs(it).roundToInt()}° ${if (abs(it) <= 3) "LEVEL" else "TILT"}" } ?: "LEVEL —", fontSize = 8.sp, lineHeight = 10.sp, color = Color.White.copy(alpha = .7f), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Mode.entries.forEach { item -> TextButton(onClick = { mode = item }, colors = ButtonDefaults.textButtonColors(contentColor = if (mode == item) Ink else Muted)) { Text(item.label, fontSize = 12.sp, fontWeight = if (mode == item) FontWeight.SemiBold else FontWeight.Normal) } }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            RoundControl(Icons.Outlined.PhotoLibrary, "Review photo") { session.latest?.let(onReview) ?: onPick() }
            Box(Modifier.size(72.dp).border(1.dp, Ink, CircleShape).padding(5.dp)) {
                FilledIconButton(onClick = { gate.reset(); session.capture() }, enabled = session.ready && !session.capturing, modifier = Modifier.fillMaxSize(), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Ink, contentColor = Paper)) {
                    if (session.capturing) CircularProgressIndicator(Modifier.size(24.dp), color = Paper, strokeWidth = 2.dp) else Icon(Icons.Outlined.CameraAlt, "Take photograph", Modifier.size(26.dp))
                }
            }
            RoundControl(Icons.Outlined.Cameraswitch, "Switch camera", session.ready && !session.capturing, session::flip)
        }
        Text(if (tick - session.capturedAt < 3000 && session.capturedAt > 0) "Saved to Pictures / PHIRA" else "A little guidance. Your own eye.", Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp), fontSize = 10.sp, lineHeight = 14.sp, color = Muted)
    }
    if (settings) ModalBottomSheet(onDismissRequest = { settings = false }, containerColor = Paper) {
        Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Make it yours", style = MaterialTheme.typography.titleLarge)
            Text("Composition guide", color = Muted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Grid.entries.forEach { option -> FilterChip(selected = option == grid, onClick = { onGrid(option) }, label = { Text(option.label) }) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Auto capture"); Text("Hold an aligned frame for 1.4 seconds.", fontSize = 11.sp, color = Muted) }
                Switch(checked = auto, onCheckedChange = onAuto)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Flash", Modifier.weight(1f))
                OutlinedButton(onClick = session::cycleFlash) { Text(when (session.flash) { ImageCapture.FLASH_MODE_ON -> "On"; ImageCapture.FLASH_MODE_AUTO -> "Auto"; else -> "Off" }) }
            }
            OutlinedButton(onClick = { settings = false; onPick() }) { Text("Choose a photo to review") }
            HorizontalDivider(color = Line)
            Text("PHIRA 0.1.1 · Early access", fontSize = 12.sp)
            Text("Faces are detected on this device, without identifying people. Object mode may need an initial model download. No account or photo uploads.\n\nFraming alignment is a geometric guide. Phi is one way to compose; trust your eye.", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RoundControl(icon: ImageVector, label: String, enabled: Boolean = true, action: () -> Unit) {
    OutlinedIconButton(onClick = action, enabled = enabled, modifier = Modifier.size(48.dp), border = BorderStroke(1.dp, Line)) { Icon(icon, label, Modifier.size(22.dp)) }
}

@Composable
private fun ViewfinderPill(text: String) {
    Text(text, Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = .45f)).padding(horizontal = 8.dp, vertical = 5.dp), color = Color.White, fontSize = 8.sp, lineHeight = 12.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
}

@Composable
private fun CompositionOverlay(grid: Grid, subjects: List<Subject>, guidance: Guidance) {
    Canvas(Modifier.fillMaxSize()) {
        val guideColor = Color.White.copy(alpha = .33f)
        grid.anchors.forEach { fraction ->
            drawLine(guideColor, Offset(size.width * fraction, 0f), Offset(size.width * fraction, size.height), strokeWidth = 1.dp.toPx())
            drawLine(guideColor, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction), strokeWidth = 1.dp.toPx())
        }
        subjects.forEach { subject ->
            val x = subject.left * size.width; val y = subject.top * size.height
            val r = subject.right * size.width; val b = subject.bottom * size.height
            val color = if (guidance.ready) Color(0xFFBED8C6) else Color.White.copy(alpha = .8f)
            val length = 12.dp.toPx()
            listOf(Offset(x, y) to Offset(1f, 1f), Offset(r, y) to Offset(-1f, 1f), Offset(x, b) to Offset(1f, -1f), Offset(r, b) to Offset(-1f, -1f)).forEach { (p, direction) ->
                drawLine(color, p, p + Offset(direction.x * length, 0f), 1.5.dp.toPx())
                drawLine(color, p, p + Offset(0f, direction.y * length), 1.5.dp.toPx())
            }
        }
        if (grid != Grid.NONE) guidance.target?.let { target ->
            val p = Offset(target.x * size.width, target.y * size.height)
            drawCircle(Color.White.copy(alpha = .8f), 5.dp.toPx(), p, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }
    }
}
