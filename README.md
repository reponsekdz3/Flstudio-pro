# FL Studio Mobile Workstation (Professional Digital Audio Workstation)

A high-performance, studio-grade Android Digital Audio Workstation (DAW) inspired by FL Studio. Built from the ground up in Kotlin with Jetpack Compose, an offline PCM/WAV floating-point audio engine, an expansive physical modeling & FM/wavetable sound vault, hardware rack styling, and low-latency interactive controls.

---

## 🌟 Key Architectural Pillars

### 1. 🎛️ Interactive Hardware Channel Rack
- **Rackmount Chassis**: Features brushed dark metal finish, rack screws, status LEDs, and channel color indicators.
- **Pattern Sequencer**: 16-step step sequencer grid with high-contrast active step sweep, beat division markers (4 beats per measure), and swing groove timing (0%–100%).
- **Micro-Controls per Channel**:
  - Direct rotary drag knob for Volume (-inf to +6 dB).
  - Direct rotary drag knob for Stereo Panning (100% Left to 100% Right).
  - Mixer Track routing LCD selector (Track 0 Master to Track 129 Insert).
  - Mute and Solo toggle buttons with active LED glow.
- **Context Workflow Actions**:
  - Fill every 2 steps, Fill every 4 steps, Fill every 8 steps.
  - Invert steps, Clear steps, Clone channel, and Delete channel.
  - Direct quick-jump to Piano Roll, 3xOSC Synthesizer, or Hardware Channel Strip Inspector.

### 2. 🎹 Hardware Piano Roll & BPM Song Production Engine
- **Hardware Synthesizer Performance Strip**:
  - Tactile Ivory & Ebony performance keyboard with pitch labels (C3, C4, C5, etc.) and real-time auditioning.
  - Hardware spring-return Pitch Bend Wheel and Modulation Wheel.
- **BPM Integration & Timing Synchronization**:
  - Real-time project BPM display with exact millisecond calculation per 1/16th step and per bar.
  - Dynamic Grid Snap: 1/16th step, 1/8th beat, 1/4 bar, 1/2 bar.
  - **Quantize to BPM Grid**: Snaps all unquantized notes directly to the selected musical subdivision.
  - **BPM Chord Stamper**: Instant harmonic chord stamping (Major Triads, Minor Triads, Major 7th, Minor 7th, Dominant 7th, Suspended 4th, Octave Stacks).
  - Octave Navigation (-2 to +2 range covering C1 through C7).
  - Integrated mini-transport controls (Play/Pause, Note Counter, Clear Notes).

### 3. 🎼 Sound Vaults & Physical Synthesizers
The workstation includes custom sound engines across multiple musical styles with zero static mocks:
- **Pianos & Keyboards**: Steinway D-274 Concert Grand Piano, Yamaha Vintage Upright, Fender Rhodes Stage 73, Wurlitzer 200A, Yamaha DX7 FM Glass Bell Piano, Celesta, Harpsichord, Church Pipe Organ, Hammond B3 Rotary Organ, Hohner Clavinet D6.
- **Strings & Violins**: Stradivarius Solo Legato Violin, Pizzicato Violin Pluck, Orchestral Cello Sustain, 64-Piece Symphony String Ensemble, Chamber Viola Vibrato, Appalachian Folk Fiddle, Contrabass Staccato, Concert Grand Harp.
- **Guitars & Basses**: 12-String Chime, Gibson Tube Overdrive Solo, Heavy Metal Palm-Muted Chug, Surf Tremolo, Spanish Flamenco Rasgueado, Delta Blues Steel Slide, Country Pedal Steel, Stratocaster Clean, Heavy Rock Marshall Distortion, Acoustic Steel-String, Spanish Classical Nylon, Funk Muted Rhythm, Fender Precision Bass.
- **Drums & Percussion**: Roland 808 & 909 kicks, snares, claps, hi-hats, open hats, reverse trap cymbals, ride cymbals, congas, bongos, cowbells, tamborines, timbales, floor toms, rack toms.
- **Amapiano & Afrobeat**: Resonant Log Drums, Amapiano Bounce Kicks, Afro Percussion, Private School Deep Kicks, Club Whistle, Clap Rolls.
- **Hip-Hop & Trap**: 808 Sub Basses, Distorted Saturated 808s, UK Drill Sliding 808s, Ghost Snares, Triplet Rolls.
- **Electronic & Dance**: Alan Walker Anthem Supersaws, Sidechain Pumping Basses, Acid TB-303, CS-80 Brass, Chiptune NES 8-bit, Dark Cyberpunk Reese Basses.
- **Brass & Woodwinds**: Miles Davis Harmon Mute Trumpet, Heavy Trombone Stabs, French Horn Swells, Silver Flutes, Tenor Saxophones, Clarinets, Oboes.
- **World & Ethnic**: Indian Classical Sitar, Japanese 13-String Koto, African Kalimba, Andean Pan Flute, Highland Bagpipes, Greek Bouzouki.

### 4. 🎚️ 130-Track Hardware Mixer & Real-Time DSP Rack
- 130 Discrete Mixer Tracks (Track 0 Master, Tracks 1–14 Submix Inserts, Tracks 15–64 Aux Busses, Tracks 65–129 Instrument Inserts).
- Complete per-track hardware DSP rack:
  - **3-Band Parametric EQ**: Low Shelf, Mid Peaking Band, High Shelf.
  - **Overdrive / Distortion**: Tube drive saturation and wet/dry mix.
  - **Stereo Chorus / Flanger**: Modulation rate, depth, and stereo spread.
  - **Tempo-Synced Stereo Delay**: Delay time in milliseconds, feedback regeneration, wet/dry blend.
  - **Studio Reverb**: Reverb room size decay, damping, and wet level.
  - **Peak Limiter / Dynamic Compressor**: Threshold dB, compression ratio, soft knee.
  - **Fruity Soft Clipper**: Master saturation limiter on Track 0.

### 5. 💾 Powerful Audio Exporter & `.rep` Project Vault
- **Offline High-Precision Audio Rendering (`AudioExporter.kt`)**:
  - Full Song mixdown, current Pattern render, or Multi-Track STEM separation.
  - Formats: WAV 16-Bit PCM, WAV 24-Bit Studio Master, WAV 32-Bit Floating Point.
  - Sample Rates: 44.1 kHz (CD Quality), 48.0 kHz (Studio Standard), 96.0 kHz (High-Resolution Audiophile).
  - Real-time rendering progress indicator with time estimation.
- **Real File System Integration & `.rep` Project Files**:
  - Complete project serialization in JSON-based `.rep` project files stored directly in external app storage (`/Android/data/.../files/FLStudioProjects`).
  - Saves all patterns, channels, note events, step states, mixer settings, BPM, and swing.
  - Project file vault browser with instant load, overwrite save, and project sharing via Android Sharesheet (`Intent.ACTION_SEND`).

---

## 🛠️ Tech Stack & Dependencies

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material 3 Dynamic Color & Studio Dark Theme)
- **Audio Synthesis Engine**: Custom Native Kotlin PCM Audio Synthesizer (`SynthVoice.kt`, `OrchestralAndKeysSynthesizer.kt`, `AudioEngine.kt`)
- **Audio Output**: Android `AudioTrack` low-latency streaming in PCM 16-bit stereo at 44.1 kHz
- **Audio File Export**: Binary WAV RIFF writer with post-render header patching
- **Architecture**: Clean Architecture MVVM with Coroutines and StateFlow
- **Serialization**: `org.json` for deterministic, portable `.rep` project data files

---

## 🚀 Getting Started & Usage

1. **Launch App**: The app opens directly to the Hardware Channel Rack.
2. **Channel Rack**:
   - Tap any step button (1 to 16) to activate rhythm triggers.
   - Drag rotary knobs to adjust volume and pan.
   - Tap the channel name or settings icon to open channel options (fill, invert, clear, clone).
3. **Hardware Piano Roll**:
   - Select the **PIANO ROLL** tab in the bottom bar.
   - Use the interactive piano keyboard or grid to enter melodic notes for any instrument.
   - Use the **CHORD STAMP** dropdown to lay down complex chord progressions in one tap.
   - Use **QUANTIZE** to snap notes to the exact project BPM grid.
4. **Mixer & DSP**:
   - Switch to the **MIXER** tab to balance levels, adjust stereo panning, and tweak EQ, Delay, Reverb, Chorus, and Compression.
5. **Project & Audio Export**:
   - Tap the **SAVE** or **EXPORT** buttons in the top transport bar.
   - Choose to save as a `.rep` project file or render offline to high-resolution 24-bit WAV.
