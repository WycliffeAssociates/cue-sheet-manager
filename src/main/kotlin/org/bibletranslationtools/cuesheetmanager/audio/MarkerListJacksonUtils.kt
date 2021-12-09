package org.bibletranslationtools.cuesheetmanager.audio

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import java.lang.IllegalArgumentException
import java.text.ParseException
import org.wycliffeassociates.otter.common.audio.AudioCue

class MarkerListDeserializer : JsonDeserializer<List<AudioCue>>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): List<AudioCue> {
        val node = p?.codec?.readTree<JsonNode>(p) ?: throw ParseException("Unable to parse json", 0)
        val cuePoints = mutableListOf<AudioCue>()
        for (key in node.fieldNames()) {
            cuePoints.add(AudioCue(node.get(key).asInt(), key))
        }
        return cuePoints
    }
}

class MarkerListSerializer : JsonSerializer<List<AudioCue>>() {
    override fun serialize(value: List<AudioCue>?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        if (value == null) throw IllegalArgumentException("Input cannot be null")
        gen?.writeStartObject()
        for (cue in value) {
            gen?.writeNumberField(cue.label, cue.location)
        }
        gen?.writeEndObject()
    }
}
