package com.example.model

import java.util.UUID

enum class InstrumentType(val displayName: String, val isMelodic: Boolean, val category: String = "Synth") {
    // Drum Hits
    KICK("Classic 808 Kick", false, "Drums"),
    AMAPIANO_KICK("Amapiano Bounce Kick", false, "Amapiano"),
    POP_SUB_KICK("Pop Chart Sub Kick", false, "Pop"),
    EDM_DROP_KICK("EDM Festival Drop Kick", false, "EDM"),
    SNARE("808 Snare Drum", false, "Drums"),
    POP_ACOUSTIC_SNARE("Pop Layered Snare", false, "Pop"),
    TRAP_SNARE_RIM("Trap Crisp Rimshot", false, "Hip-Hop"),
    CLAP("Studio Handclap", false, "Drums"),
    AMAPIANO_CLAP_ROLL("Amapiano Clap Roll", false, "Amapiano"),
    HIHAT_CLOSED("Hi-Hat Closed", false, "Drums"),
    HIHAT_OPEN("Hi-Hat Open", false, "Drums"),
    TRAP_HIHAT_ROLL("Trap Triplet Hi-Hat", false, "Hip-Hop"),
    AMAPIANO_SHAKER("Afro Amapiano Shaker", false, "Amapiano"),
    AMAPIANO_WHISTLE("Amapiano Club Whistle", true, "Amapiano"),
    AFRO_PERCUSSION("Afro Tribal Percussion", false, "Amapiano"),

    // 808s & Basses
    BASS_808("Classic 808 Sub Bass", true, "Bass"),
    AMAPIANO_LOG_DRUM("Amapiano Resonant Log Drum", true, "Amapiano"),
    DRAKE_DISTORTED_808("Drake Distorted Saturated 808", true, "Hip-Hop"),
    DRILL_808_SLIDE("UK Drill Sliding 808", true, "Bass"),
    EDM_SIDECHAIN_BASS("EDM Pumping Sidechain Bass", true, "EDM"),
    SYNTH_SUB_BASS("Analog Sub Bass Rumble", true, "Bass"),
    GUITAR_BASS("Fender Precision Electric Bass", true, "Guitars"),

    // Synths, Leads & Keys
    SYNTH_LEAD("Fruity 3xOSC Analog Lead", true, "Synth"),
    EDM_SUPERSAW("Alan Walker Anthem Supersaw", true, "EDM"),
    POP_BRIGHT_SYNTH("Katy Perry Bright Pop Brass", true, "Pop"),
    DRAKE_UNDERWATER_KEYS("Drake Underwater Moody Keys", true, "Hip-Hop"),
    PLUCK("Fruity Synth Pluck", true, "Synth"),
    EDM_PLUCK("Alan Walker Melodic Pluck", true, "EDM"),
    SYNTH_PAD("Lush Ambient Analog Pad", true, "Synth"),
    DRAKE_VOCAL_PAD("Moody Ambient Vocal Pad", true, "Hip-Hop"),
    BRASS_STAB("Orchestral Trap Brass Stab", true, "Hip-Hop"),
    BELL_CHIME("Crystal Bell Chime", true, "Synth"),

    // Guitars
    GUITAR_ELECTRIC_CLEAN("Electric Clean Stratocaster", true, "Guitars"),
    GUITAR_DISTORTION("Heavy Rock Marshall Distortion", true, "Guitars"),
    GUITAR_ACOUSTIC("Acoustic Steel-String Guitar", true, "Guitars"),
    GUITAR_NYLON("Spanish Classical Nylon Guitar", true, "Guitars"),
    GUITAR_FUNK("Funk Muted Rhythm Guitar", true, "Guitars"),

    // Vocals & FX
    POP_VOCAL_LEAD("Pop Vocal Formant Hook", true, "Pop"),
    MIC_SAMPLE("Audio Vocal / Mic Sample", false, "Vocals"),
    FX_RISER("EDM Festival White Noise Riser", false, "FX"),
    FX_CRASH("Stadium Crash Cymbal", false, "FX"),

    // Expanded Kicks Vault
    KICK_909_HEAVY("Roland 909 Heavy Techno Kick", false, "Drums"),
    KICK_ACOUSTIC_STUDIO("Ludwig Vintage Studio Kick", false, "Drums"),
    KICK_TRAP_SHORT("Short Hard Punch Trap Kick", false, "Hip-Hop"),
    KICK_SUB_HEAVY("Ultra Low 30Hz Sub Kick", false, "Bass"),
    KICK_AMAPIANO_DEEP("Deep Afro Private School Kick", false, "Amapiano"),
    KICK_EDM_PUNCH("Hardstyle Distorted Punch Kick", false, "EDM"),
    KICK_LOFI_WARM("Lo-Fi Tape Saturated Kick", false, "Hip-Hop"),
    KICK_DRILL_KNOCK("UK Drill Knocking Heavy Kick", false, "Hip-Hop"),
    KICK_PUNCHY_POP("Modern Radio Chart Pop Kick", false, "Pop"),
    KICK_GABBER_DISTORTED("Industrial Hardcore Distorted Kick", false, "EDM"),

    // Expanded Snares & Claps Vault
    SNARE_909_PUNCH("Roland 909 Analog Snare", false, "Drums"),
    SNARE_ACOUSTIC_CRISP("Vintage Studio Wood Snare", false, "Drums"),
    SNARE_TRAP_GHOST("Trap Layered Ghost Snare", false, "Hip-Hop"),
    SNARE_DUBSTEP_HEAVY("Dubstep Metallic Heavy Snare", false, "EDM"),
    SNARE_LOFI_BRUSH("Jazz Brush Muted Snare", false, "Drums"),
    CLAP_LAYERED_STADIUM("Big Room Stadium Handclap", false, "EDM"),
    CLAP_RETRO_80S("Linndrum 80s Synthpop Clap", false, "Pop"),
    CLAP_SNAP_FINGER("Acoustic Studio Finger Snap", false, "Pop"),

    // Expanded Percussion & Shakers
    PERC_CONGA_HIGH("Cuban Conga High Slap", false, "Drums"),
    PERC_BONGO_ROLL("Afro Bongo Tribal Roll", false, "Amapiano"),
    PERC_COWBELL_808("Classic 808 Membrane Cowbell", true, "Drums"),
    PERC_TAMBOURINE("Live Acoustic Tambourine", false, "Pop"),
    PERC_TIMBALE_LATIN("Latin Fiesta Brass Timbale", false, "Drums"),
    HIHAT_TRAP_REVERSE("Trap Reversed Cymbal Choke", false, "Hip-Hop"),
    RIDE_JAZZ_CYMBAL("Zildjian Dark Jazz Ride Cymbal", false, "Drums"),
    TOM_FLOOR_BOOM("Deep Floor Tom Resonant Boom", false, "Drums"),
    TOM_RACK_DRUM("Yamaha Rack Tom Drum", false, "Drums"),

    // Strings & Violins Vault
    VIOLIN_SOLO_EXPRESSIVE("Stradivarius Solo Violin Legato", true, "Strings"),
    VIOLIN_PIZZICATO("Concert Pizzicato Violin Pluck", true, "Strings"),
    CELLO_ORCHESTRAL_DEEP("Full Orchestral Cello Sustain", true, "Strings"),
    ORCHESTRAL_STRING_ENSEMBLE("Symphony 64-Piece String Section", true, "Strings"),
    VIOLA_WARM_VIBRATO("Warm Chamber Viola Vibrato", true, "Strings"),
    FIDDLE_FOLK_FAST("Appalachian Folk Fast Fiddle", true, "Strings"),
    CONTRABASS_STACCATO("Orchestral Double Bass Staccato", true, "Strings"),
    HARP_CONCERT_GLISS("Concert Grand Harp Pluck", true, "Strings"),

    // Pianos & Keyboards Vault
    PIANO_STEINWAY_GRAND("Steinway D-274 Concert Grand Piano", true, "Pianos"),
    PIANO_VINTAGE_UPRIGHT("Yamaha Vintage Upright Honky-Tonk", true, "Pianos"),
    KEYS_RHODES_STAGE_73("Fender Rhodes Stage 73 Warm EP", true, "Pianos"),
    KEYS_WURLITZER_200A("Wurlitzer 200A Tremolo EP", true, "Pianos"),
    KEYS_FM_BELL_DX7("Yamaha DX7 Glass Bell Piano", true, "Pianos"),
    KEYS_CELESTA_DREAM("Tchaikovsky Fairy Celesta Chime", true, "Pianos"),
    KEYS_HARPSICHORD_BAROQUE("Baroque Double Harpsichord", true, "Pianos"),
    KEYS_CHURCH_PIPE_ORGAN("Notre-Dame Grand Pipe Organ", true, "Pianos"),
    KEYS_HAMMOND_B3("Hammond B3 Rotary Leslie Organ", true, "Pianos"),
    KEYS_CLAVINET_D6("Hohner Clavinet D6 Funk Wah", true, "Pianos"),

    // Expanded Guitars Vault
    GUITAR_12_STRING_CHIME("Rickenbacker 12-String Chime", true, "Guitars"),
    GUITAR_OVERDRIVE_SOLO("Gibson Les Paul Tube Overdrive Solo", true, "Guitars"),
    GUITAR_MUTED_PALM("Heavy Metal Palm Muted Chug", true, "Guitars"),
    GUITAR_SURF_TREMOLO("Fender Jazzmaster Surf Tremolo", true, "Guitars"),
    GUITAR_FLAMENCO_RASGUEADO("Spanish Flamenco Rasgueado", true, "Guitars"),
    GUITAR_SLIDE_BLUES("Delta Blues Steel Slide Guitar", true, "Guitars"),
    GUITAR_PEDAL_STEEL("Country Pedal Steel Glissando", true, "Guitars"),

    // Brass & Woodwinds Vault
    BRASS_TRUMPET_SOLO("Miles Davis Harmon Mute Trumpet", true, "Brass & Winds"),
    BRASS_TROMBONE_STAB("Heavy Orchestral Trombone Hit", true, "Brass & Winds"),
    BRASS_FRENCH_HORN("Cinematic French Horn Swell", true, "Brass & Winds"),
    WOODWIND_FLUTE_JAZZ("Concert Silver Flute Legato", true, "Brass & Winds"),
    WOODWIND_SAX_TENOR("Smoky Midnight Tenor Saxophone", true, "Brass & Winds"),
    WOODWIND_CLARINET("Warm Orchestral Bb Clarinet", true, "Brass & Winds"),
    WOODWIND_OBOE("Symphony Pastoral Oboe", true, "Brass & Winds"),

    // World & Ethnic Vault
    ETHNIC_SITAR_INDIAN("Indian Classical Sitar Drone", true, "Ethnic"),
    ETHNIC_KOTO_JAPANESE("Japanese 13-String Koto Pluck", true, "Ethnic"),
    ETHNIC_KALIMBA_THUMB("African Thumb Kalimba Bell", true, "Ethnic"),
    ETHNIC_PAN_FLUTE("Andean Highland Pan Flute", true, "Ethnic"),
    ETHNIC_BAGPIPES_HIGHLAND("Scottish Highland Bagpipes Drone", true, "Ethnic"),
    ETHNIC_BOUZOUKI_GREEK("Mediterranean Greek Bouzouki", true, "Ethnic"),

    // Expanded Synths, Leads & Choirs Vault
    SYNTH_CHIP_8BIT("NES 8-Bit Chiptune Square Arp", true, "Synth"),
    SYNTH_ACID_303("Roland TB-303 Resonant Acid Bassline", true, "Synth"),
    SYNTH_WARM_BRASS_CS80("Yamaha CS-80 Blade Runner Brass", true, "Synth"),
    SYNTH_ETHEREAL_CHOIR("Fairlight Ethereal Vocal Choir", true, "Synth"),
    SYNTH_RETROWAVE_PLUCK("80s Synthwave Juno-106 Arp", true, "Synth"),
    SYNTH_CYBERPUNK_REESE("Dark Cyberpunk Saturated Reese", true, "Synth"),
    SYNTH_THEREMIN_GHOST("Eerie Sci-Fi Sine Theremin", true, "Synth")
}

enum class OscType(val displayName: String) {
    SINE("Sine"),
    SAWTOOTH("Saw"),
    SQUARE("Square"),
    TRIANGLE("Triangle"),
    NOISE("Noise")
}

enum class FilterType(val displayName: String) {
    LOW_PASS("Low-Pass"),
    HIGH_PASS("High-Pass"),
    BAND_PASS("Band-Pass")
}

enum class PlayMode {
    PATTERN, SONG
}

data class NoteEvent(
    val id: String = UUID.randomUUID().toString(),
    val pitch: Int = 60, // MIDI note: 60 = C4, 69 = A4 (440Hz)
    val startStep: Int = 0,
    val durationSteps: Int = 2,
    val velocity: Float = 0.85f
)

data class Channel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: InstrumentType,
    val colorHex: Long,
    val volume: Float = 0.8f,
    val pan: Float = 0.0f, // -1.0 (L) to +1.0 (R)
    val pitchSemi: Int = 0, // -24 to +24 semitones
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val steps: BooleanArray = BooleanArray(16) { false },
    val notes: List<NoteEvent> = emptyList(),
    // Synthesizer & Filter params (FL 3xOSC & Fruity Filter)
    val osc1Type: OscType = OscType.SAWTOOTH,
    val osc2Type: OscType = OscType.SQUARE,
    val osc2Mix: Float = 0.4f,
    val detuneCents: Float = 10f, // -50 to +50 cents
    val filterType: FilterType = FilterType.LOW_PASS,
    val cutoffHz: Float = 4500f,
    val resonanceQ: Float = 1.8f,
    // ADSR Envelope
    val attackMs: Float = 10f,
    val decayMs: Float = 120f,
    val sustainLevel: Float = 0.6f,
    val releaseMs: Float = 200f,
    // Mixer routing
    val mixerTrackIndex: Int = 0, // 0 = Master, 1..5 = Inserts
    // Recorded audio clip data (if type == MIC_SAMPLE)
    val sampleBuffer: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Channel

        if (id != other.id) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (colorHex != other.colorHex) return false
        if (volume != other.volume) return false
        if (pan != other.pan) return false
        if (pitchSemi != other.pitchSemi) return false
        if (isMuted != other.isMuted) return false
        if (isSolo != other.isSolo) return false
        if (!steps.contentEquals(other.steps)) return false
        if (notes != other.notes) return false
        if (osc1Type != other.osc1Type) return false
        if (osc2Type != other.osc2Type) return false
        if (osc2Mix != other.osc2Mix) return false
        if (detuneCents != other.detuneCents) return false
        if (filterType != other.filterType) return false
        if (cutoffHz != other.cutoffHz) return false
        if (resonanceQ != other.resonanceQ) return false
        if (attackMs != other.attackMs) return false
        if (decayMs != other.decayMs) return false
        if (sustainLevel != other.sustainLevel) return false
        if (releaseMs != other.releaseMs) return false
        if (mixerTrackIndex != other.mixerTrackIndex) return false
        if (sampleBuffer != null) {
            if (other.sampleBuffer == null) return false
            if (!sampleBuffer.contentEquals(other.sampleBuffer)) return false
        } else if (other.sampleBuffer != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + colorHex.hashCode()
        result = 31 * result + steps.contentHashCode()
        result = 31 * result + notes.hashCode()
        return result
    }
}

data class Pattern(
    val id: Int = 1,
    val name: String = "Pattern 1",
    val colorHex: Long = 0xFFFF7300,
    val channelSteps: Map<String, BooleanArray> = emptyMap(),
    val channelNotes: Map<String, List<NoteEvent>> = emptyMap()
)

data class TimelineTrack(
    val id: Int,
    val name: String,
    val colorHex: Long,
    val category: String = "BEATS",
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val volume: Float = 1.0f
)

data class SongMarker(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bar: Int,
    val colorHex: Long = 0xFFFF9E0B
)

data class PatternClip(
    val id: String = UUID.randomUUID().toString(),
    val patternIndex: Int = 0,
    val patternName: String = "Pattern 1",
    val colorHex: Long = 0xFFFF7300,
    val trackIndex: Int = 0, // Track 0 to 15
    val startBar: Int = 0, // 0 to 31
    val lengthBars: Int = 1,
    val isMuted: Boolean = false
)

data class MixerTrack(
    val id: Int, // 0 = Master, 1..14 = Inserts, 15..64 = Aux Send Busses 1 to 50
    val name: String,
    val isAuxBus: Boolean = false,
    val auxBusIndex: Int = 0, // 1 to 50 if isAuxBus
    val volumeDb: Float = 0.0f, // -60dB to +6dB
    val pan: Float = 0.0f, // -1.0 to +1.0
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    // Aux Send routing: maps auxBusTrackId (15..64) -> sendLevel (0f to 1f)
    val auxSends: Map<Int, Float> = emptyMap(),
    // Fruity Parametric EQ
    val eqEnabled: Boolean = true,
    val eqLowGain: Float = 0.0f, // -12 to +12 dB
    val eqMidGain: Float = 0.0f,
    val eqHighGain: Float = 0.0f,
    // Fruity Delay
    val delayEnabled: Boolean = false,
    val delayTimeMs: Float = 250f,
    val delayFeedback: Float = 0.4f,
    val delayWet: Float = 0.35f,
    // Fruity Reverb
    val reverbEnabled: Boolean = false,
    val reverbRoom: Float = 0.65f,
    val reverbWet: Float = 0.30f,
    // Fruity Fast Dist
    val distEnabled: Boolean = false,
    val distDrive: Float = 2.5f,
    val distMix: Float = 0.0f,
    // Vintage Analog Chorus / Flanger
    val chorusEnabled: Boolean = false,
    val chorusDepth: Float = 0.5f,
    val chorusRate: Float = 1.2f,
    val chorusMix: Float = 0.0f,
    // Vintage Hardware Compressor / Limiter
    val compEnabled: Boolean = false,
    val compThresholdDb: Float = -12f,
    val compRatio: Float = 4.0f,
    // Live metering levels
    val peakLeft: Float = 0.0f,
    val peakRight: Float = 0.0f
)
