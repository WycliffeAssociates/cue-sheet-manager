package org.bibletranslationtools.cuesheetmanager

import com.matthewrussell.trwav.*
import org.opf_labs.audio.*
import java.io.File
import java.io.OutputStreamWriter

class InvalidWavFileException(message: String? = null) : java.lang.Exception(message)

class CueSheetWriter private constructor() {

    private lateinit var wav: File
    private lateinit var wavFile: WavFile
    private lateinit var cueFile: File
    private val mapper = MetadataMapper()
    private val cueSheet: CueSheet = CueSheet()

    @Throws(InvalidWavFileException::class)
    constructor(wav: File, cueFile: File? = null) : this() {
        this.wav = wav

        if (wav.extension.toLowerCase().endsWith("wav").not()) {
            throw InvalidWavFileException("Not a wav file")
        }

        if (cueFile == null) {
            this.cueFile = File(wav.absolutePath.replaceAfterLast(".", "cue"))
        }
    }

    private fun readWavFile() {
        wavFile = WavFileReader(wav).read()
        wavFile.metadata.markers.sortBy { it.location }
    }

    private fun initCueSheet() {
        cueSheet.comment = mapper.toJSON(wavFile.metadata)
        cueSheet.title = listOf(wavFile.metadata.language, wavFile.metadata.anthology, wavFile.metadata.slug)
            .joinToString("_")
        val fileData = FileData(cueSheet, wav.name, "WAVE")
        for ((i, cue) in wavFile.metadata.markers.withIndex()) {
            val cueNumber = findCueNumber(cue.label, i)
            val trackData = TrackData(fileData, cueNumber, "AUDIO")
            trackData.title = cue.label
            val index = Index()
            index.number = 1
            index.position = position(cue.location, SAMPLE_RATE)
            trackData.indices.add(index)

            fileData.trackData.add(trackData)
        }
        cueSheet.fileData.add(fileData)
    }

    fun write() {
        readWavFile()
        initCueSheet()

        val sheet = CueSheetSerializer().serializeCueSheet(cueSheet)
        cueFile.outputStream().use { fos ->
            OutputStreamWriter(fos, "UTF-8").use { osw ->
                osw.write(sheet)
            }
        }
    }

    private fun position(sampleNumber: Int, sampleRate: Int): Position {
        val totalSeconds = (sampleNumber / sampleRate)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val frames = (sampleNumber % sampleRate * 75 / sampleRate)
        return Position(minutes, seconds, frames)
    }

    private fun findCueNumber(label: String, index: Int): Int {
        var labelNumber = label.toIntOrNull()
        if (labelNumber == null) {
            labelNumber = index + 1
        }
        return labelNumber
    }
}