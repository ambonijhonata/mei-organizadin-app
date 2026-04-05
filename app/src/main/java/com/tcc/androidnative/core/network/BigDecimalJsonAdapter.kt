package com.tcc.androidnative.core.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.math.BigDecimal

class BigDecimalJsonAdapter : JsonAdapter<BigDecimal>() {
    override fun fromJson(reader: JsonReader): BigDecimal? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            JsonReader.Token.STRING,
            JsonReader.Token.NUMBER -> {
                val raw = reader.nextString()
                raw.toBigDecimalOrNull()
                    ?: throw JsonDataException("Expected BigDecimal but was '$raw' at path ${reader.path}")
            }
            else -> throw JsonDataException(
                "Expected NUMBER or STRING but was ${reader.peek()} at path ${reader.path}"
            )
        }
    }

    override fun toJson(writer: JsonWriter, value: BigDecimal?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.value(value.toPlainString())
    }
}
