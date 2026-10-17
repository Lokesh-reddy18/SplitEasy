package com.split.easy.data

import java.util.UUID

/** Client-generated ids, so records can be created offline and synced without clashes. */
fun interface IdGenerator {
    fun newId(): String
}

class UuidGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
