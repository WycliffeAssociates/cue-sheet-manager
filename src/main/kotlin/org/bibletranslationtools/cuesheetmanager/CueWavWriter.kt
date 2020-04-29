package org.bibletranslationtools.cuesheetmanager

import com.matthewrussell.trwav.*
import org.opf_labs.audio.CueParser
import org.opf_labs.audio.CueSheet
import org.opf_labs.audio.TrackData
import java.io.File
import java.io.IOException

class CueWavWriter private constructor() {

    private lateinit var cueFile: File
    private lateinit var wav: File
    private val mapper = MetadataMapper()
    private lateinit var wavFile: WavFile
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
        val tempWavFile = WavFileReader(wav).read()
        val metadata = mapper.fromJSON(cueSheet.comment)

        // Rebuild verse markers from the cue sheet
        metadata.markers = arrayListOf()
        for (track: TrackData in fileData.trackData) {
            val position = track.indices.first().position.totalFrames / 75 * SAMPLE_RATE
            metadata.markers.add(
                CuePoint(
                    position,
                    track.number.toString()
                )
            )
        }

        wavFile = WavFile(metadata, tempWavFile.audio)
    }

    fun write() {
        WavFileWriter(mapper).write(wavFile, wav)
    }
}