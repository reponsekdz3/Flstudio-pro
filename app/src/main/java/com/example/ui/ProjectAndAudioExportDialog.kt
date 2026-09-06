package com.example.ui

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.audio.*
import com.example.project.ProjectSerializer
import com.example.project.RepProjectFile
import com.example.ui.theme.*
import com.example.viewmodel.FlStudioViewModel
import kotlinx.coroutines.launch
import java.io.File

private enum class ExportHubTab(val title: String) {
    AUDIO_EXPORT("Audio Master (WAV)"),
    REP_PROJECT("Save / Share .rep"),
    PROJECT_VAULT("Saved Projects")
}

@Composable
fun ProjectAndAudioExportDialog(
    viewModel: FlStudioViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    var activeTab by remember { mutableStateOf(ExportHubTab.AUDIO_EXPORT) }
    var projectName by remember { mutableStateOf(uiState.projectPresetName.ifBlank { "My_FL_Studio_Project" }) }

    // Audio export state
    var selectedFormat by remember { mutableStateOf(ExportAudioFormat.WAV_16BIT) }
    var selectedRange by remember { mutableStateOf(ExportRange.FULL_SONG) }
    var selectedSampleRate by remember { mutableIntStateOf(44100) }
    var isRendering by remember { mutableStateOf(false) }
    var renderProgress by remember { mutableFloatStateOf(0f) }
    var renderedResult by remember { mutableStateOf<AudioExportResult?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isAudioPreviewPlaying by remember { mutableStateOf(false) }

    // Project files state
    var savedProjects by remember { mutableStateOf(emptyList<RepProjectFile>()) }
    var lastSavedRepFile by remember { mutableStateOf<File?>(null) }

    fun refreshSavedProjects() {
        savedProjects = ProjectSerializer.listSavedProjects(context)
    }

    LaunchedEffect(Unit) {
        refreshSavedProjects()
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Import external .rep file launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    val loaded = ProjectSerializer.deserializeProject(content)
                    viewModel.loadCustomProject(loaded)
                    Toast.makeText(context, "Loaded .rep project: ${loaded.projectName}", Toast.LENGTH_LONG).show()
                    onDismiss()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading .rep file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareFile(file: File, mimeType: String, chooserTitle: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isRendering) {
                mediaPlayer?.stop()
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(6.dp),
            colors = CardDefaults.cardColors(containerColor = StudioPanel),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header Deck
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HardwareRackScrew(modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PROJECT & AUDIO EXPORT HUB",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = FruityOrange
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(FruityOrange.copy(alpha = 0.2f))
                                        .border(0.5.dp, FruityOrange, RoundedCornerShape(3.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "REAL FILE SYSTEM",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = FruityOrange
                                    )
                                }
                            }
                            Text(
                                text = "Export 16/24/32-Bit Master WAVs, track stems, or save & share .rep project files",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            mediaPlayer?.stop()
                            onDismiss()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF13161D), RoundedCornerShape(6.dp))
                        .padding(3.dp)
                ) {
                    ExportHubTab.values().forEach { tab ->
                        val isSelected = activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) FruityOrange else Color.Transparent)
                                .clickable { activeTab = tab }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.title.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                when (activeTab) {
                    ExportHubTab.AUDIO_EXPORT -> {
                        AudioExportContent(
                            projectName = projectName,
                            onProjectNameChange = { projectName = it },
                            selectedFormat = selectedFormat,
                            onFormatChange = { selectedFormat = it },
                            selectedRange = selectedRange,
                            onRangeChange = { selectedRange = it },
                            selectedSampleRate = selectedSampleRate,
                            onSampleRateChange = { selectedSampleRate = it },
                            isRendering = isRendering,
                            renderProgress = renderProgress,
                            renderedResult = renderedResult,
                            isAudioPreviewPlaying = isAudioPreviewPlaying,
                            onStartRender = {
                                coroutineScope.launch {
                                    isRendering = true
                                    renderProgress = 0f
                                    renderedResult = null
                                    try {
                                        val result = AudioExporter.renderAudioOffline(
                                            context = context,
                                            projectName = projectName,
                                            bpm = uiState.bpm,
                                            swing = uiState.swing,
                                            playMode = uiState.playMode,
                                            totalBars = uiState.totalBars,
                                            channels = uiState.channels,
                                            patterns = uiState.patterns,
                                            selectedPatternId = uiState.selectedPatternId,
                                            playlistClips = uiState.playlistClips,
                                            mixerTracks = uiState.mixerTracks,
                                            format = selectedFormat,
                                            range = selectedRange,
                                            sampleRate = selectedSampleRate,
                                            onProgress = { p -> renderProgress = p }
                                        )
                                        renderedResult = result
                                        Toast.makeText(context, "Render complete! Master saved to disk.", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Render error: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isRendering = false
                                    }
                                }
                            },
                            onPlayPreview = { file ->
                                try {
                                    if (mediaPlayer?.isPlaying == true) {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        isAudioPreviewPlaying = false
                                    } else {
                                        mediaPlayer?.release()
                                        mediaPlayer = MediaPlayer().apply {
                                            setDataSource(file.absolutePath)
                                            prepare()
                                            start()
                                            setOnCompletionListener {
                                                isAudioPreviewPlaying = false
                                            }
                                        }
                                        isAudioPreviewPlaying = true
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Playback error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShareAudio = { file ->
                                shareFile(file, "audio/wav", "Share Master Audio Track")
                            }
                        )
                    }

                    ExportHubTab.REP_PROJECT -> {
                        RepProjectContent(
                            projectName = projectName,
                            onProjectNameChange = { projectName = it },
                            lastSavedFile = lastSavedRepFile,
                            onSaveRep = {
                                val jsonStr = ProjectSerializer.serializeProject(
                                    projectName = projectName,
                                    bpm = uiState.bpm,
                                    swing = uiState.swing,
                                    playMode = uiState.playMode,
                                    totalBars = uiState.totalBars,
                                    channels = uiState.channels,
                                    patterns = uiState.patterns,
                                    selectedPatternId = uiState.selectedPatternId,
                                    mixerTracks = uiState.mixerTracks,
                                    playlistClips = uiState.playlistClips
                                )
                                val savedFile = ProjectSerializer.saveProjectToFile(context, projectName, jsonStr)
                                lastSavedRepFile = savedFile
                                refreshSavedProjects()
                                Toast.makeText(context, "Saved .rep project to ${savedFile.name}", Toast.LENGTH_SHORT).show()
                            },
                            onShareRep = { file ->
                                shareFile(file, "application/octet-stream", "Share FL Studio .rep Project")
                            },
                            onImportPicker = {
                                importLauncher.launch(arrayOf("*/*"))
                            }
                        )
                    }

                    ExportHubTab.PROJECT_VAULT -> {
                        ProjectVaultContent(
                            savedProjects = savedProjects,
                            onLoadProject = { repFile ->
                                try {
                                    val content = repFile.file.readText(Charsets.UTF_8)
                                    val loaded = ProjectSerializer.deserializeProject(content)
                                    viewModel.loadCustomProject(loaded)
                                    Toast.makeText(context, "Loaded: ${loaded.projectName}", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to load project: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShareProject = { repFile ->
                                shareFile(repFile.file, "application/octet-stream", "Share .rep Project")
                            },
                            onDeleteProject = { repFile ->
                                ProjectSerializer.deleteProject(repFile.file)
                                refreshSavedProjects()
                                Toast.makeText(context, "Deleted project", Toast.LENGTH_SHORT).show()
                            },
                            onImportPicker = {
                                importLauncher.launch(arrayOf("*/*"))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioExportContent(
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    selectedFormat: ExportAudioFormat,
    onFormatChange: (ExportAudioFormat) -> Unit,
    selectedRange: ExportRange,
    onRangeChange: (ExportRange) -> Unit,
    selectedSampleRate: Int,
    onSampleRateChange: (Int) -> Unit,
    isRendering: Boolean,
    renderProgress: Float,
    renderedResult: AudioExportResult?,
    isAudioPreviewPlaying: Boolean,
    onStartRender: () -> Unit,
    onPlayPreview: (File) -> Unit,
    onShareAudio: (File) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Project Name Input
        OutlinedTextField(
            value = projectName,
            onValueChange = onProjectNameChange,
            label = { Text("Master Track / File Name", fontSize = 11.sp) },
            singleLine = true,
            enabled = !isRendering,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = FruityOrange,
                unfocusedBorderColor = StudioBorder
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Audio Bit Depth Format
        Text(
            text = "AUDIO MASTER FORMAT & BIT DEPTH",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ExportAudioFormat.values().forEach { fmt ->
                val isSelected = selectedFormat == fmt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) FruityOrange.copy(alpha = 0.2f) else Color(0xFF141820))
                        .border(1.dp, if (isSelected) FruityOrange else StudioBorder, RoundedCornerShape(6.dp))
                        .clickable(enabled = !isRendering) { onFormatChange(fmt) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${fmt.bitDepth}-BIT WAV",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) FruityOrange else TextPrimary
                        )
                        Text(
                            text = if (fmt == ExportAudioFormat.WAV_16BIT) "Standard CD" else if (fmt == ExportAudioFormat.WAV_24BIT) "Hi-Res Studio" else "DAW 32 Float",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Render Range
        Text(
            text = "RENDER SOURCE & SCOPE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ExportRange.values().forEach { range ->
                val isSelected = selectedRange == range
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) FruityCyan.copy(alpha = 0.2f) else Color(0xFF141820))
                        .border(1.dp, if (isSelected) FruityCyan else StudioBorder, RoundedCornerShape(6.dp))
                        .clickable(enabled = !isRendering) { onRangeChange(range) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (range == ExportRange.FULL_SONG) "Full Song" else if (range == ExportRange.CURRENT_PATTERN) "Pattern Loop" else "All Stems",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) FruityCyan else TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Offline Rendering Status / Progress
        if (isRendering) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1218)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FruityOrange))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = FruityOrange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RENDERING OFFLINE AUDIO MASTER...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FruityOrange
                            )
                        }
                        Text(
                            text = "${(renderProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = FruityOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { renderProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = FruityOrange,
                        trackColor = Color(0xFF1A1F29)
                    )
                }
            }
        } else if (renderedResult != null) {
            val res = renderedResult
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1813)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FruityLime))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FruityLime, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MASTER EXPORTED SUCCESSFULLY!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = FruityLime
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "File: ${res.file.name} (${String.format("%.1f", res.fileSizeBytes / 1024f / 1024f)} MB)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Text(
                        text = "Format: ${res.format.displayName} | ${res.sampleRate} Hz Stereo",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onPlayPreview(res.file) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = if (isAudioPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = FruityCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAudioPreviewPlaying) "Stop" else "Play Audio",
                                fontSize = 10.sp,
                                color = FruityCyan
                            )
                        }

                        Button(
                            onClick = { onShareAudio(res.file) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FruityLime)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share WAV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Big Bounce Action Button
        Button(
            onClick = onStartRender,
            enabled = !isRendering && projectName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FruityOrange,
                disabledContainerColor = FruityOrange.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "BOUNCE & RENDER MASTER AUDIO (WAV)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun RepProjectContent(
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    lastSavedFile: File?,
    onSaveRep: () -> Unit,
    onShareRep: (File) -> Unit,
    onImportPicker: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = projectName,
            onValueChange = onProjectNameChange,
            label = { Text("Project Name (.rep)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = FruityOrange,
                unfocusedBorderColor = StudioBorder
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141720)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "WHAT IS .REP PROJECT FORMAT?",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = FruityOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The .rep format is a complete, lossless snapshot of your song: all channel synth setups, ADSR envelopes, filters, patterns, 16-step sequencer tracks, piano roll note events, playlist arrangement clips, and 129-track mixer FX chains. Saving .rep allows continuing and backing up projects anytime.",
                    fontSize = 9.5.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (lastSavedFile != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1813)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FruityLime))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PROJECT SAVED TO DISK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FruityLime
                        )
                        Text(
                            text = lastSavedFile.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = { onShareRep(lastSavedFile) },
                        colors = ButtonDefaults.buttonColors(containerColor = FruityLime)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share .rep", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onImportPicker,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FruityCyan)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = FruityCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open External .rep", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FruityCyan)
            }

            Button(
                onClick = onSaveRep,
                enabled = projectName.isNotBlank(),
                modifier = Modifier
                    .weight(1.2f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FruityOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SAVE .REP TO DISK", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
        }
    }
}

@Composable
private fun ProjectVaultContent(
    savedProjects: List<RepProjectFile>,
    onLoadProject: (RepProjectFile) -> Unit,
    onShareProject: (RepProjectFile) -> Unit,
    onDeleteProject: (RepProjectFile) -> Unit,
    onImportPicker: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${savedProjects.size} SAVED .REP PROJECTS ON DEVICE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )

            Button(
                onClick = onImportPicker,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = FruityCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import from Device", fontSize = 9.sp, color = FruityCyan)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (savedProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("No saved .rep projects found on disk.", fontSize = 11.sp, color = TextMuted)
                    Text("Save your current project or import an existing .rep file.", fontSize = 9.5.sp, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(savedProjects) { rep ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StudioPanelLight),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rep.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${rep.bpm} BPM",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FruityOrange
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${rep.channelCount} Channels",
                                        fontSize = 9.sp,
                                        color = FruityCyan
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = rep.formattedDate,
                                        fontSize = 9.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onShareProject(rep) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { onDeleteProject(rep) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }

                                Button(
                                    onClick = { onLoadProject(rep) },
                                    colors = ButtonDefaults.buttonColors(containerColor = FruityOrange),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("LOAD", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
