package org.bibletranslationtools.cuesheetmanager.audio

import org.wycliffeassociates.otter.common.audio.wav.InvalidWavFileException
import org.wycliffeassociates.otter.common.audio.wav.RiffChunk
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

const val CHUNK_HEADER_SIZE = 8
const val CHUNK_LABEL_SIZE = 4
const val DWORD_SIZE = 4
const val LIST_LABEL = "LIST"
const val INFO_LABEL = "INFO"
const val IART_LABEL = "IART"

class BttrChunk : RiffChunk {

    var metadata: BttrMetadata = BttrMetadata()
    private val metadataMapper: BttrMetadataMapper = BttrMetadataMapper()

    override val totalSize: Int
        get(): Int {
            val metadataString = metadataMapper.toJSON(metadata)
            val paddedString = wordAlignString(metadataString)
            val size = CHUNK_HEADER_SIZE + CHUNK_LABEL_SIZE + paddedString.length
            return CHUNK_HEADER_SIZE + size
        }

    override fun toByteArray(): ByteArray {
        val metadataString = metadataMapper.toJSON(metadata)
        val paddedString = wordAlignString(metadataString)
        val size = CHUNK_HEADER_SIZE + CHUNK_LABEL_SIZE + paddedString.length
        val buffer = ByteBuffer.allocate(size + CHUNK_HEADER_SIZE)
        buffer
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(LIST_LABEL.toByteArray(Charset.forName("US-ASCII")))
            .putInt(CHUNK_HEADER_SIZE + CHUNK_LABEL_SIZE + paddedString.length)
            .put((INFO_LABEL + IART_LABEL).toByteArray(Charset.forName("US-ASCII")))
            .putInt(paddedString.length)
            .put(paddedString.toByteArray(Charset.forName("US-ASCII")))
        return buffer.array()
    }

    override fun parse(chunk: ByteBuffer) {
        chunk.order(ByteOrder.LITTLE_ENDIAN)
        while (chunk.remaining() > CHUNK_HEADER_SIZE) {
            val subchunkLabel = chunk.getText(CHUNK_LABEL_SIZE)
            val subchunkSize = chunk.int

            if (chunk.remaining() < subchunkSize) {
                throw InvalidWavFileException(
                    "Chunk $subchunkLabel is of size: $subchunkSize but remaining chunk size is ${chunk.remaining()}"
                )
            }

            // section off a buffer from this one that can be used for parsing the nested chunk
            val buffer = chunk.slice()
            buffer.limit(buffer.position() + subchunkSize)

            when (subchunkLabel) {
                LIST_LABEL -> parseMetadata(buffer)
                else -> Unit
            }

            // move on to the next chunk
            chunk.seek(subchunkSize)
        }
    }

    private fun parseMetadata(chunk: ByteBuffer) {
        chunk.order(ByteOrder.LITTLE_ENDIAN)

        // Skip List Chunks that are not subtype "INFO"
        val chunkFinished = chunk.remaining() < CHUNK_LABEL_SIZE
        val notInfoLabel = INFO_LABEL != chunk.getText(CHUNK_LABEL_SIZE)
        if (chunkFinished || notInfoLabel) {
            return
        }

        while (chunk.remaining() > CHUNK_HEADER_SIZE) {
            val subchunk = chunk.getText(CHUNK_LABEL_SIZE)
            val subchunkSize = chunk.int
            when (subchunk) {
                IART_LABEL -> {
                    val labelBytes = ByteArray(subchunkSize)
                    chunk.get(labelBytes)
                    val meta = String(labelBytes, Charsets.US_ASCII)
                    parseJson(meta)
                }
                else -> {
                    chunk.seek(subchunkSize)
                }
            }
        }
    }

    private fun parseJson(json: String) {
        metadata = try {
            metadataMapper.fromJSON(json)
        } catch (e: Exception) {
            BttrMetadata()
        }
    }

    private fun wordAlignedLength(length: Int): Int {
        return length + (DWORD_SIZE - length % DWORD_SIZE)
    }

    private fun wordAlignString(str: String): String = str.padEnd(
        wordAlignedLength(str.length),
        0.toChar()
    )
}
