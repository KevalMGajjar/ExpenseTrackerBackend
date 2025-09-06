package com.example.splitwiseclonebackend.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.bson.types.ObjectId
import java.io.IOException

// This class teaches Jackson how to convert a complex ObjectId into a simple String.
class ObjectIdSerializer : JsonSerializer<ObjectId>() {
    @Throws(IOException::class)
    override fun serialize(value: ObjectId?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        if (value == null) {
            gen?.writeNull()
        } else {
            // When asked to serialize an ObjectId, simply write its hexadecimal string representation.
            gen?.writeString(value.toHexString())
        }
    }
}