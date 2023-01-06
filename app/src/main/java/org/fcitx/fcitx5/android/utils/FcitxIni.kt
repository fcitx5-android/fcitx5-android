package org.fcitx.fcitx5.android.utils

import org.ini4j.Config
import org.ini4j.Ini
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.URL

class FcitxIni() : Ini() {
    init {
        val config = Config.getGlobal().clone()
        config.isStrictOperator = true
        config.isEscape = false
        config.isEscapeNewline = false
        setConfig(config)
    }

    constructor(file: File) : this() {
        setFile(file)
        load()
    }

    constructor(input: URL) : this() {
        load(input)
    }

    constructor(input: InputStream) : this() {
        load(input)
    }

    constructor(input: Reader) : this() {
        load(input)
    }
}