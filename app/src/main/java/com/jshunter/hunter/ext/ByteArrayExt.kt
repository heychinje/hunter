package com.jshunter.hunter.ext

import java.nio.charset.Charset

fun ByteArray.toText(
    offset: Int = 0, length: Int = size - 1, charset: Charset = Charsets.UTF_8
) = String(this, offset, length, charset)