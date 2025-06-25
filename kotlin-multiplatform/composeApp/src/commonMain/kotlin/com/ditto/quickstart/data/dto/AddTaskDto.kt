package com.ditto.quickstart.data.dto

import com.ditto.kotlin.serialization.DittoCborSerializable
import com.ditto.kotlin.serialization.DittoCborSerializable.Utf8String

data class AddTaskDto(
    val title: String,
    val done: Boolean,
    val deleted: Boolean,
)

fun AddTaskDto.toDittoDictionary() = DittoCborSerializable.Dictionary(mapOf(
    Utf8String("title") to Utf8String(title),
    Utf8String("done") to DittoCborSerializable.BooleanValue(done),
    Utf8String("deleted") to DittoCborSerializable.BooleanValue(deleted),
))


