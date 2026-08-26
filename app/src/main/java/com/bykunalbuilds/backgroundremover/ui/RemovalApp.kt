package com.bykunalbuilds.backgroundremover.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bykunalbuilds.backgroundremover.inference.OnnxBackgroundRemover
import com.bykunalbuilds.backgroundremover.ui.theme.checkerDark
import com.bykunalbuilds.backgroundremover.ui.theme.checkerLight
import kotlin.math.max
import kotlin.math.min

@Composable
fun RemovalApp(
    state: RemovalUiState,
    snackbarHostState: SnackbarHostState,
    onSelectImage: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onChooseAnother: () -> Unit,
    onOpenInstagram: () -> Unit,
) {
    var showAbout by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            AppHeader(onInfo = { showAbout = true })
            AnimatedContent(
                targetState = when {
                    state.hasResult -> Screen.RESULT
                    state.isProcessing -> Screen.PROCESSING
                    else -> Screen.EMPTY
                },
                label = "main state",
                modifier = Modifier.weight(1f),
            ) { screen ->
                when (screen) {
                    Screen.EMPTY -> EmptyScreen(onSelectImage)
                    Screen.PROCESSING -> ProcessingScreen(state.original)
                    Screen.RESULT -> ResultScreen(
                        original = requireNotNull(state.original),
                        result = requireNotNull(state.result),
                        isSaving = state.isSaving,
                        saved = state.savedUri != null,
                        onSave = onSave,
                        onShare = onShare,
                        onChooseAnother = onChooseAnother,
                    )
                }
            }
        }
    }
    if (showAbout) {
        AboutDialog(
            onDismiss = { showAbout = false },
            onOpenInstagram = onOpenInstagram,
        )
    }
}

private enum class Screen { EMPTY, PROCESSING, RESULT }

@Composable
private fun AppHeader(onInfo: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppMark(40.dp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "AI Background Remover",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = buildAnnotatedString {
                        append("by ")
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                        ) {
                            append("Kunal Builds")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    letterSpacing = .15.sp,
                )
            }
            IconButton(onClick = onInfo) {
                Icon(Icons.Outlined.Info, contentDescription = "About")
            }
        }
    }
}

@Composable
private fun AppMark(size: androidx.compose.ui.unit.Dp) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val accentColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size / 4),
        color = MaterialTheme.colorScheme.onBackground,
    ) {
        Canvas(Modifier.padding(size / 5)) {
            val gap = this.size.width * .12f
            val square = (this.size.width - gap) / 2f
            listOf(
                Offset.Zero,
                Offset(square + gap, 0f),
                Offset(0f, square + gap),
            ).forEach { origin ->
                drawRect(backgroundColor, origin, Size(square, square))
            }
            drawRect(accentColor, Offset(square + gap, square + gap), Size(square, square))
        }
    }
}

@Composable
private fun EmptyScreen(onSelectImage: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        val wide = maxWidth >= 720.dp
        if (wide) {
            Row(
                modifier = Modifier.widthIn(max = 1_020.dp).padding(vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroCopy(Modifier.weight(1f))
                SelectPanel(onSelectImage, Modifier.weight(.9f))
            }
        } else {
            Column(
                modifier = Modifier.widthIn(max = 560.dp).padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HeroCopy(Modifier.fillMaxWidth())
                Spacer(Modifier.height(32.dp))
                SelectPanel(onSelectImage, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HeroCopy(modifier: Modifier = Modifier) {
    Column(modifier) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("ON-DEVICE • PRIVATE", fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = "Remove the background.\nKeep every detail.",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 44.sp,
            lineHeight = 48.sp,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Choose a photo and get a clean, full-resolution transparent PNG. No upload, account, watermark, or waiting on a server.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            FeatureTick("Free")
            FeatureTick("Open source")
            FeatureTick("Offline")
        }
    }
}

@Composable
private fun FeatureTick(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SelectPanel(onSelectImage: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .semantics { role = Role.Button; contentDescription = "Select an image" },
        onClick = onSelectImage,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.padding(18.dp).size(34.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(22.dp))
            Text("Start with a photo", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "JPEG, PNG, or WebP • processed only on this device",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSelectImage,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null)
                Spacer(Modifier.width(9.dp))
                Text("Select an image", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProcessingScreen(original: Bitmap?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .heightIn(min = 280.dp, max = 560.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (original != null) {
                    Image(
                        original.asImageBitmap(),
                        contentDescription = "Selected photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                    Spacer(Modifier.height(18.dp))
                    Text("Removing background…", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(7.dp))
                    Text("Everything stays on this device", color = Color.White.copy(alpha = .76f), fontSize = 13.sp)
                }
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            }
        }
    }
}

@Composable
private fun ResultScreen(
    original: Bitmap,
    result: Bitmap,
    isSaving: Boolean,
    saved: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onChooseAnother: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val wide = maxWidth >= 840.dp
        if (wide) {
            Row(
                modifier = Modifier.widthIn(max = 1_140.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BeforeAfterPreview(original, result, Modifier.weight(1.5f))
                ResultActions(isSaving, saved, onSave, onShare, onChooseAnother, Modifier.weight(.7f))
            }
        } else {
            Column(modifier = Modifier.widthIn(max = 720.dp)) {
                BeforeAfterPreview(original, result, Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                ResultActions(isSaving, saved, onSave, onShare, onChooseAnother, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BeforeAfterPreview(original: Bitmap, result: Bitmap, modifier: Modifier = Modifier) {
    var split by remember { mutableFloatStateOf(.5f) }
    val animatedSplit by animateFloatAsState(split, label = "comparison split")
    val ratio = original.width.toFloat() / original.height.coerceAtLeast(1)
    Surface(
        modifier = modifier
            .aspectRatio(ratio.coerceIn(.62f, 1.7f))
            .heightIn(min = 300.dp, max = 650.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        split = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                }
                .semantics { contentDescription = "Before and after comparison. Drag horizontally." },
        ) {
            Checkerboard(Modifier.fillMaxSize())
            Image(
                result.asImageBitmap(),
                contentDescription = "Transparent result",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Image(
                original.asImageBitmap(),
                contentDescription = "Original photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().drawWithContent {
                    clipRect(right = size.width * animatedSplit) { this@drawWithContent.drawContent() }
                },
            )
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width * animatedSplit
                drawLine(Color.White.copy(alpha = .95f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 3.dp.toPx())
                drawCircle(Color.White, radius = 20.dp.toPx(), center = Offset(x, size.height / 2f))
                drawCircle(Color(0xFF172019), radius = 17.dp.toPx(), center = Offset(x, size.height / 2f))
                drawLine(Color.White, Offset(x - 6.dp.toPx(), size.height / 2f), Offset(x + 6.dp.toPx(), size.height / 2f), 2.dp.toPx())
            }
            LabelPill("BEFORE", Modifier.align(Alignment.TopStart).padding(14.dp))
            LabelPill("AFTER", Modifier.align(Alignment.TopEnd).padding(14.dp))
        }
    }
}

@Composable
private fun Checkerboard(modifier: Modifier = Modifier) {
    val light = MaterialTheme.colorScheme.checkerLight
    val dark = MaterialTheme.colorScheme.checkerDark
    Canvas(modifier) {
        drawRect(light)
        val cell = 16.dp.toPx()
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) 0f else cell
            while (x < size.width) {
                drawRect(dark, Offset(x, y), Size(cell, cell))
                x += cell * 2
            }
            row++
            y += cell
        }
    }
}

@Composable
private fun LabelPill(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = CircleShape, color = Color.Black.copy(alpha = .64f), contentColor = Color.White) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultActions(
    isSaving: Boolean,
    saved: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onChooseAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
            Row(Modifier.padding(horizontal = 11.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Check, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(5.dp))
                Text("BACKGROUND REMOVED", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Your PNG is ready", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(7.dp))
        Text("The checkerboard shows real transparency. Drag the divider to compare the original.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onSave,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (isSaving) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
            else Icon(if (saved) Icons.Outlined.Check else Icons.Outlined.Download, null)
            Spacer(Modifier.width(8.dp))
            Text(if (saved) "Save another copy" else "Save PNG")
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = onShare, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.IosShare, null)
                Spacer(Modifier.width(7.dp))
                Text("Share")
            }
            TextButton(onClick = onChooseAnother, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Refresh, null)
                Spacer(Modifier.width(7.dp))
                Text("Choose another")
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit, onOpenInstagram: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppMark(44.dp) },
        title = { Text("AI Background Remover", textAlign = TextAlign.Center) },
        text = {
            Column {
                Text("Built by Kunal Builds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text("Free, open source, and private by design. Photos are processed locally and are never uploaded or tracked.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))
                HorizontalDivider()
                Spacer(Modifier.height(18.dp))
                Text("BUILT WITH", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("Kotlin • Jetpack Compose • ONNX Runtime\n${OnnxBackgroundRemover.MODEL_NAME}", fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenInstagram) {
                Text("@bykunalbuilds")
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Outlined.ArrowOutward, null, Modifier.size(17.dp))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
