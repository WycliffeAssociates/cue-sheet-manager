package org.bibletranslationtools.cuesheetmanager

import org.bibletranslationtools.cuesheetmanager.audio.BttrChunk
import org.bibletranslationtools.cuesheetmanager.audio.BttrMetadataMapper
import org.opf_labs.audio.*
import org.wycliffeassociates.otter.common.audio.AudioCue
import org.wycliffeassociates.otter.common.audio.DEFAULT_SAMPLE_RATE
import org.wycliffeassociates.otter.common.audio.wav.WavFile
import org.wycliffeassociates.otter.common.audio.wav.WavMetadata
import java.io.File
import java.io.OutputStreamWriter
import java.text.MessageFormat

class InvalidWavFileException(message: String? = null) : java.lang.Exception(message)

class CueSheetWriter private constructor() {

    private lateinit var wav: File
    private lateinit var wavFile: WavFile
    private lateinit var bttrChunk: BttrChunk
    private lateinit var cueFile: File
    private val cueSheet: CueSheet = CueSheet()

    @Throws(InvalidWavFileException::class)
    constructor(wav: File, cueFile: File? = null) : this() {
        this.wav = wav

        if (wav.extension.lowercase().endsWith("wav").not()) {
            throw InvalidWavFileException("Not a wav file")
        }

        if (cueFile == null) {
            this.cueFile = File(wav.absolutePath.replaceAfterLast(".", "cue"))
        }
    }

    private fun readWavFile() {
        bttrChunk = BttrChunk()
        val metadata = WavMetadata(listOf(bttrChunk))
        wavFile = WavFile(wav, metadata)
    }

    private fun initCueSheet() {
        val mapper = BttrMetadataMapper()
        cueSheet.comment = mapper.toJSON(bttrChunk.metadata)

        val title = MessageFormat.format(
            "{0}_{1}_{2}",
            bttrChunk.metadata.language,
            bttrChunk.metadata.anthology,
            bttrChunk.metadata.slug
        )
        cueSheet.title = "\"$title\""
        val fileData = FileData(cueSheet, "\"${wav.name}\"", "WAVE")

        val otterWavFile = org.wycliffeassociates.otter.common.audio.wav.WavFile(wav)
        val markers = otterWavFile.metadata.getCues().map { AudioCue(it.location, it.label) }

        for ((i, cue) in markers.withIndex()) {
            val cueNumber = findCueNumber(cue.label, i)
            val trackData = TrackData(fileData, cueNumber, "AUDIO")
            trackData.title = cue.label
            val index = Index()
            index.number = 1
            index.position = position(cue.location, DEFAULT_SAMPLE_RATE)
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
