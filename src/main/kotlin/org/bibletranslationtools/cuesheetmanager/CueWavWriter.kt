package org.bibletranslationtools.cuesheetmanager

import org.bibletranslationtools.cuesheetmanager.audio.BttrChunk
import org.opf_labs.audio.CueParser
import org.opf_labs.audio.CueSheet
import org.opf_labs.audio.TrackData
import org.wycliffeassociates.otter.common.audio.AudioCue
import org.wycliffeassociates.otter.common.audio.DEFAULT_SAMPLE_RATE
import org.wycliffeassociates.otter.common.audio.wav.VerseMarkerChunk
import org.wycliffeassociates.otter.common.audio.wav.WavFile
import org.wycliffeassociates.otter.common.audio.wav.WavMetadata
import org.wycliffeassociates.otter.common.audio.wav.WavOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

const val WAV_HEADER_SIZE = 44

class CueWavWriter private constructor() {

    private lateinit var cueFile: File
    private lateinit var wav: File
    private lateinit var wavFile: WavFile
    private lateinit var audioData: ByteArray
    lateinit var cueSheet: CueSheet
        private set

    @Throws(IOException::class)
    constructor(cueFile: File) : this() {
        this.cueFile = cueFile
        initWavFile()
    }

    private fun initWavFile() {
        this.cueSheet = CueParser.parse(cueFile)
        val fileData = cueSheet.fileData.first()
        wav = File(cueFile.parent, fileData.file)

        val bttrChunk = BttrChunk()
        val wavMetadata = WavMetadata(listOf(bttrChunk))
        val tempWavFile = WavFile(wav, wavMetadata)
        audioData = ByteArray(tempWavFile.totalAudioLength)
        RandomAccessFile(wav, "r").use {
            it.skipBytes(WAV_HEADER_SIZE)
            it.read(audioData)
        }

        // Rebuild verse markers from the cue sheet
        bttrChunk.metadata.markers = arrayListOf()
        for (track: TrackData in fileData.trackData) {
            val position = track.indices.first().position.totalFrames / 75 * DEFAULT_SAMPLE_RATE
            bttrChunk.metadata.markers.add(
                AudioCue(position, track.number.toString())
            )
        }
        val cueChunk = VerseMarkerChunk()
        bttrChunk.metadata.markers.map {
            cueChunk.addCue(it)
        }

        wavFile = WavFile(
            wav,
            tempWavFile.channels,
            tempWavFile.sampleRate,
            tempWavFile.bitsPerSample,
            WavMetadata(listOf(bttrChunk, cueChunk))
        )
    }

    fun write() {
        WavOutputStream(wavFile).use {
            it.write(audioData)
        }
    }
}
