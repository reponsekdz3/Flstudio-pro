package com.example.project

import android.content.Context
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class RepProjectFile(
    val file: File,
    val name: String,
    val bpm: Int,
    val channelCount: Int,
    val lastModified: Long,
    val formattedDate: String
)

data class LoadedProjectData(
    val projectName: String,
    val bpm: Int,
    val swing: Int,
    val playMode: PlayMode,
    val totalBars: Int,
    val channels: List<Channel>,
    val patterns: List<Pattern>,
    val selectedPatternId: Int,
    val mixerTracks: List<MixerTrack>,
    val playlistClips: List<PatternClip>
)

object ProjectSerializer {

    private const val FILE_EXTENSION = ".rep"
    private const val PROJECTS_DIR = "rep_projects"

    fun getProjectsDir(context: Context): File {
        val dir = File(context.filesDir, PROJECTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Serializes complete project state into JSON string
     */
    fun serializeProject(
        projectName: String,
        bpm: Int,
        swing: Int,
        playMode: PlayMode,
        totalBars: Int,
        channels: List<Channel>,
        patterns: List<Pattern>,
        selectedPatternId: Int,
        mixerTracks: List<MixerTrack>,
        playlistClips: List<PatternClip>
    ): String {
        val root = JSONObject()
        root.put("format", "REP_STUDIO_PROJECT")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("projectName", projectName)
        root.put("bpm", bpm)
        root.put("swing", swing)
        root.put("playMode", playMode.name)
        root.put("totalBars", totalBars)
        root.put("selectedPatternId", selectedPatternId)

        // Channels
        val channelsArray = JSONArray()
        for (channel in channels) {
            val chObj = JSONObject()
            chObj.put("id", channel.id)
            chObj.put("name", channel.name)
            chObj.put("type", channel.type.name)
            chObj.put("colorHex", channel.colorHex)
            chObj.put("volume", channel.volume.toDouble())
            chObj.put("pan", channel.pan.toDouble())
            chObj.put("pitchSemi", channel.pitchSemi)
            chObj.put("isMuted", channel.isMuted)
            chObj.put("isSolo", channel.isSolo)
            chObj.put("mixerTrackIndex", channel.mixerTrackIndex)

            // ADSR & Synth
            chObj.put("osc1Type", channel.osc1Type.name)
            chObj.put("osc2Type", channel.osc2Type.name)
            chObj.put("osc2Mix", channel.osc2Mix.toDouble())
            chObj.put("detuneCents", channel.detuneCents.toDouble())
            chObj.put("filterType", channel.filterType.name)
            chObj.put("cutoffHz", channel.cutoffHz.toDouble())
            chObj.put("resonanceQ", channel.resonanceQ.toDouble())
            chObj.put("attackMs", channel.attackMs.toDouble())
            chObj.put("decayMs", channel.decayMs.toDouble())
            chObj.put("sustainLevel", channel.sustainLevel.toDouble())
            chObj.put("releaseMs", channel.releaseMs.toDouble())

            // Steps
            val stepsArray = JSONArray()
            for (step in channel.steps) {
                stepsArray.put(step)
            }
            chObj.put("steps", stepsArray)

            // Notes
            val notesArray = JSONArray()
            for (note in channel.notes) {
                val nObj = JSONObject()
                nObj.put("id", note.id)
                nObj.put("pitch", note.pitch)
                nObj.put("startStep", note.startStep)
                nObj.put("durationSteps", note.durationSteps)
                nObj.put("velocity", note.velocity.toDouble())
                notesArray.put(nObj)
            }
            chObj.put("notes", notesArray)

            channelsArray.put(chObj)
        }
        root.put("channels", channelsArray)

        // Patterns
        val patternsArray = JSONArray()
        for (pattern in patterns) {
            val pObj = JSONObject()
            pObj.put("id", pattern.id)
            pObj.put("name", pattern.name)
            pObj.put("colorHex", pattern.colorHex)

            // Channel steps map
            val stepsMapObj = JSONObject()
            for ((chId, steps) in pattern.channelSteps) {
                val arr = JSONArray()
                for (s in steps) arr.put(s)
                stepsMapObj.put(chId, arr)
            }
            pObj.put("channelSteps", stepsMapObj)

            // Channel notes map
            val notesMapObj = JSONObject()
            for ((chId, noteList) in pattern.channelNotes) {
                val arr = JSONArray()
                for (note in noteList) {
                    val nObj = JSONObject()
                    nObj.put("id", note.id)
                    nObj.put("pitch", note.pitch)
                    nObj.put("startStep", note.startStep)
                    nObj.put("durationSteps", note.durationSteps)
                    nObj.put("velocity", note.velocity.toDouble())
                    arr.put(nObj)
                }
                notesMapObj.put(chId, arr)
            }
            pObj.put("channelNotes", notesMapObj)

            patternsArray.put(pObj)
        }
        root.put("patterns", patternsArray)

        // Playlist Clips
        val clipsArray = JSONArray()
        for (clip in playlistClips) {
            val cObj = JSONObject()
            cObj.put("id", clip.id)
            cObj.put("patternIndex", clip.patternIndex)
            cObj.put("patternName", clip.patternName)
            cObj.put("colorHex", clip.colorHex)
            cObj.put("trackIndex", clip.trackIndex)
            cObj.put("startBar", clip.startBar)
            cObj.put("lengthBars", clip.lengthBars)
            cObj.put("isMuted", clip.isMuted)
            clipsArray.put(cObj)
        }
        root.put("playlistClips", clipsArray)

        // Mixer Tracks (Master + Inserts)
        val mixerArray = JSONArray()
        for (track in mixerTracks) {
            val mObj = JSONObject()
            mObj.put("id", track.id)
            mObj.put("name", track.name)
            mObj.put("isAuxBus", track.isAuxBus)
            mObj.put("auxBusIndex", track.auxBusIndex)
            mObj.put("volumeDb", track.volumeDb.toDouble())
            mObj.put("pan", track.pan.toDouble())
            mObj.put("isMuted", track.isMuted)
            mObj.put("isSolo", track.isSolo)

            // EQ
            mObj.put("eqEnabled", track.eqEnabled)
            mObj.put("eqLowGain", track.eqLowGain.toDouble())
            mObj.put("eqMidGain", track.eqMidGain.toDouble())
            mObj.put("eqHighGain", track.eqHighGain.toDouble())

            // Delay
            mObj.put("delayEnabled", track.delayEnabled)
            mObj.put("delayTimeMs", track.delayTimeMs.toDouble())
            mObj.put("delayFeedback", track.delayFeedback.toDouble())
            mObj.put("delayWet", track.delayWet.toDouble())

            // Reverb
            mObj.put("reverbEnabled", track.reverbEnabled)
            mObj.put("reverbRoom", track.reverbRoom.toDouble())
            mObj.put("reverbWet", track.reverbWet.toDouble())

            // Distortion
            mObj.put("distEnabled", track.distEnabled)
            mObj.put("distDrive", track.distDrive.toDouble())
            mObj.put("distMix", track.distMix.toDouble())

            // Chorus
            mObj.put("chorusEnabled", track.chorusEnabled)
            mObj.put("chorusDepth", track.chorusDepth.toDouble())
            mObj.put("chorusRate", track.chorusRate.toDouble())
            mObj.put("chorusMix", track.chorusMix.toDouble())

            // Compressor
            mObj.put("compEnabled", track.compEnabled)
            mObj.put("compThresholdDb", track.compThresholdDb.toDouble())
            mObj.put("compRatio", track.compRatio.toDouble())

            // Aux sends
            val sendsObj = JSONObject()
            for ((auxId, level) in track.auxSends) {
                sendsObj.put(auxId.toString(), level.toDouble())
            }
            mObj.put("auxSends", sendsObj)

            mixerArray.put(mObj)
        }
        root.put("mixerTracks", mixerArray)

        return root.toString(2)
    }

    /**
     * Saves project to real file system under projects directory with .rep extension
     */
    fun saveProjectToFile(
        context: Context,
        projectName: String,
        jsonContent: String
    ): File {
        val cleanName = projectName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = if (cleanName.endsWith(FILE_EXTENSION, ignoreCase = true)) cleanName else "$cleanName$FILE_EXTENSION"
        val projectsDir = getProjectsDir(context)
        val targetFile = File(projectsDir, fileName)

        FileOutputStream(targetFile).use { output ->
            output.write(jsonContent.toByteArray(Charsets.UTF_8))
            output.flush()
        }
        return targetFile
    }

    /**
     * Deserializes JSON string back into complete project data
     */
    fun deserializeProject(jsonString: String): LoadedProjectData {
        val root = JSONObject(jsonString)
        val projectName = root.optString("projectName", "Untitled Project")
        val bpm = root.optInt("bpm", 130)
        val swing = root.optInt("swing", 0)
        val playModeStr = root.optString("playMode", PlayMode.PATTERN.name)
        val playMode = try { PlayMode.valueOf(playModeStr) } catch (_: Exception) { PlayMode.PATTERN }
        val totalBars = root.optInt("totalBars", 16)
        val selectedPatternId = root.optInt("selectedPatternId", 1)

        // Channels
        val channels = mutableListOf<Channel>()
        val channelsArray = root.optJSONArray("channels") ?: JSONArray()
        for (i in 0 until channelsArray.length()) {
            val chObj = channelsArray.getJSONObject(i)
            val typeStr = chObj.optString("type", "SYNTH_LEAD")
            val type = try { InstrumentType.valueOf(typeStr) } catch (_: Exception) { InstrumentType.SYNTH_LEAD }

            val stepsArray = chObj.optJSONArray("steps")
            val steps = BooleanArray(16) { idx ->
                if (stepsArray != null && idx < stepsArray.length()) stepsArray.optBoolean(idx, false) else false
            }

            val notes = mutableListOf<NoteEvent>()
            val notesArray = chObj.optJSONArray("notes")
            if (notesArray != null) {
                for (nIdx in 0 until notesArray.length()) {
                    val nObj = notesArray.getJSONObject(nIdx)
                    notes.add(
                        NoteEvent(
                            id = nObj.optString("id", UUID.randomUUID().toString()),
                            pitch = nObj.optInt("pitch", 60),
                            startStep = nObj.optInt("startStep", 0),
                            durationSteps = nObj.optInt("durationSteps", 2),
                            velocity = nObj.optDouble("velocity", 0.85).toFloat()
                        )
                    )
                }
            }

            val osc1Str = chObj.optString("osc1Type", OscType.SAWTOOTH.name)
            val osc2Str = chObj.optString("osc2Type", OscType.SQUARE.name)
            val filterStr = chObj.optString("filterType", FilterType.LOW_PASS.name)

            channels.add(
                Channel(
                    id = chObj.optString("id", UUID.randomUUID().toString()),
                    name = chObj.optString("name", "Channel"),
                    type = type,
                    colorHex = chObj.optLong("colorHex", 0xFFFF7300),
                    volume = chObj.optDouble("volume", 0.8).toFloat(),
                    pan = chObj.optDouble("pan", 0.0).toFloat(),
                    pitchSemi = chObj.optInt("pitchSemi", 0),
                    isMuted = chObj.optBoolean("isMuted", false),
                    isSolo = chObj.optBoolean("isSolo", false),
                    steps = steps,
                    notes = notes,
                    osc1Type = try { OscType.valueOf(osc1Str) } catch (_: Exception) { OscType.SAWTOOTH },
                    osc2Type = try { OscType.valueOf(osc2Str) } catch (_: Exception) { OscType.SQUARE },
                    osc2Mix = chObj.optDouble("osc2Mix", 0.4).toFloat(),
                    detuneCents = chObj.optDouble("detuneCents", 10.0).toFloat(),
                    filterType = try { FilterType.valueOf(filterStr) } catch (_: Exception) { FilterType.LOW_PASS },
                    cutoffHz = chObj.optDouble("cutoffHz", 4500.0).toFloat(),
                    resonanceQ = chObj.optDouble("resonanceQ", 1.8).toFloat(),
                    attackMs = chObj.optDouble("attackMs", 10.0).toFloat(),
                    decayMs = chObj.optDouble("decayMs", 120.0).toFloat(),
                    sustainLevel = chObj.optDouble("sustainLevel", 0.6).toFloat(),
                    releaseMs = chObj.optDouble("releaseMs", 200.0).toFloat(),
                    mixerTrackIndex = chObj.optInt("mixerTrackIndex", 0)
                )
            )
        }

        // Patterns
        val patterns = mutableListOf<Pattern>()
        val patternsArray = root.optJSONArray("patterns")
        if (patternsArray != null && patternsArray.length() > 0) {
            for (pIdx in 0 until patternsArray.length()) {
                val pObj = patternsArray.getJSONObject(pIdx)
                val patId = pObj.optInt("id", pIdx + 1)
                val patName = pObj.optString("name", "Pattern $patId")
                val patColor = pObj.optLong("colorHex", 0xFFFF7300)

                val stepMap = mutableMapOf<String, BooleanArray>()
                val stepsMapObj = pObj.optJSONObject("channelSteps")
                if (stepsMapObj != null) {
                    val keys = stepsMapObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val arr = stepsMapObj.getJSONArray(key)
                        val bArr = BooleanArray(16) { bIdx -> if (bIdx < arr.length()) arr.optBoolean(bIdx, false) else false }
                        stepMap[key] = bArr
                    }
                }

                val notesMap = mutableMapOf<String, List<NoteEvent>>()
                val notesMapObj = pObj.optJSONObject("channelNotes")
                if (notesMapObj != null) {
                    val keys = notesMapObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val arr = notesMapObj.getJSONArray(key)
                        val nList = mutableListOf<NoteEvent>()
                        for (i in 0 until arr.length()) {
                            val nObj = arr.getJSONObject(i)
                            nList.add(
                                NoteEvent(
                                    id = nObj.optString("id", UUID.randomUUID().toString()),
                                    pitch = nObj.optInt("pitch", 60),
                                    startStep = nObj.optInt("startStep", 0),
                                    durationSteps = nObj.optInt("durationSteps", 2),
                                    velocity = nObj.optDouble("velocity", 0.85).toFloat()
                                )
                            )
                        }
                        notesMap[key] = nList
                    }
                }

                patterns.add(
                    Pattern(
                        id = patId,
                        name = patName,
                        colorHex = patColor,
                        channelSteps = stepMap,
                        channelNotes = notesMap
                    )
                )
            }
        }

        // Playlist Clips
        val clips = mutableListOf<PatternClip>()
        val clipsArray = root.optJSONArray("playlistClips")
        if (clipsArray != null) {
            for (cIdx in 0 until clipsArray.length()) {
                val cObj = clipsArray.getJSONObject(cIdx)
                clips.add(
                    PatternClip(
                        id = cObj.optString("id", UUID.randomUUID().toString()),
                        patternIndex = cObj.optInt("patternIndex", 0),
                        patternName = cObj.optString("patternName", "Pattern"),
                        colorHex = cObj.optLong("colorHex", 0xFFFF7300),
                        trackIndex = cObj.optInt("trackIndex", 0),
                        startBar = cObj.optInt("startBar", 0),
                        lengthBars = cObj.optInt("lengthBars", 1),
                        isMuted = cObj.optBoolean("isMuted", false)
                    )
                )
            }
        }

        // Mixer Tracks
        val mixerTracks = mutableListOf<MixerTrack>()
        val mixerArray = root.optJSONArray("mixerTracks")
        if (mixerArray != null) {
            for (mIdx in 0 until mixerArray.length()) {
                val mObj = mixerArray.getJSONObject(mIdx)
                val auxMap = mutableMapOf<Int, Float>()
                val sendsObj = mObj.optJSONObject("auxSends")
                if (sendsObj != null) {
                    val keys = sendsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val auxId = key.toIntOrNull() ?: continue
                        auxMap[auxId] = sendsObj.optDouble(key, 0.0).toFloat()
                    }
                }

                mixerTracks.add(
                    MixerTrack(
                        id = mObj.optInt("id", mIdx),
                        name = mObj.optString("name", if (mIdx == 0) "Master" else "Insert $mIdx"),
                        isAuxBus = mObj.optBoolean("isAuxBus", false),
                        auxBusIndex = mObj.optInt("auxBusIndex", 0),
                        volumeDb = mObj.optDouble("volumeDb", 0.0).toFloat(),
                        pan = mObj.optDouble("pan", 0.0).toFloat(),
                        isMuted = mObj.optBoolean("isMuted", false),
                        isSolo = mObj.optBoolean("isSolo", false),
                        auxSends = auxMap,
                        eqEnabled = mObj.optBoolean("eqEnabled", true),
                        eqLowGain = mObj.optDouble("eqLowGain", 0.0).toFloat(),
                        eqMidGain = mObj.optDouble("eqMidGain", 0.0).toFloat(),
                        eqHighGain = mObj.optDouble("eqHighGain", 0.0).toFloat(),
                        delayEnabled = mObj.optBoolean("delayEnabled", false),
                        delayTimeMs = mObj.optDouble("delayTimeMs", 250.0).toFloat(),
                        delayFeedback = mObj.optDouble("delayFeedback", 0.4).toFloat(),
                        delayWet = mObj.optDouble("delayWet", 0.35).toFloat(),
                        reverbEnabled = mObj.optBoolean("reverbEnabled", false),
                        reverbRoom = mObj.optDouble("reverbRoom", 0.65).toFloat(),
                        reverbWet = mObj.optDouble("reverbWet", 0.30).toFloat(),
                        distEnabled = mObj.optBoolean("distEnabled", false),
                        distDrive = mObj.optDouble("distDrive", 2.5).toFloat(),
                        distMix = mObj.optDouble("distMix", 0.0).toFloat(),
                        chorusEnabled = mObj.optBoolean("chorusEnabled", false),
                        chorusDepth = mObj.optDouble("chorusDepth", 0.5).toFloat(),
                        chorusRate = mObj.optDouble("chorusRate", 1.2).toFloat(),
                        chorusMix = mObj.optDouble("chorusMix", 0.0).toFloat(),
                        compEnabled = mObj.optBoolean("compEnabled", false),
                        compThresholdDb = mObj.optDouble("compThresholdDb", -12.0).toFloat(),
                        compRatio = mObj.optDouble("compRatio", 4.0).toFloat()
                    )
                )
            }
        }

        return LoadedProjectData(
            projectName = projectName,
            bpm = bpm,
            swing = swing,
            playMode = playMode,
            totalBars = totalBars,
            channels = channels,
            patterns = if (patterns.isNotEmpty()) patterns else listOf(Pattern(1, "Pattern 1")),
            selectedPatternId = selectedPatternId,
            mixerTracks = mixerTracks,
            playlistClips = clips
        )
    }

    /**
     * Lists all .rep project files saved on device
     */
    fun listSavedProjects(context: Context): List<RepProjectFile> {
        val dir = getProjectsDir(context)
        val files = dir.listFiles { _, name -> name.endsWith(FILE_EXTENSION, ignoreCase = true) } ?: return emptyList()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        return files.map { file ->
            var channelCount = 0
            var bpm = 130
            try {
                val content = file.readText(Charsets.UTF_8)
                val json = JSONObject(content)
                bpm = json.optInt("bpm", 130)
                channelCount = json.optJSONArray("channels")?.length() ?: 0
            } catch (_: Exception) {}

            RepProjectFile(
                file = file,
                name = file.nameWithoutExtension,
                bpm = bpm,
                channelCount = channelCount,
                lastModified = file.lastModified(),
                formattedDate = dateFormat.format(Date(file.lastModified()))
            )
        }.sortedByDescending { it.lastModified }
    }

    /**
     * Deletes a .rep project file
     */
    fun deleteProject(file: File): Boolean {
        return try {
            file.delete()
        } catch (_: Exception) {
            false
        }
    }
}
