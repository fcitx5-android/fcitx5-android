package org.fcitx.fcitx5.android.data.quickphrase

import java.io.File

class BuiltinQuickPhrase(
    override val file: File,
    // always be .mb (not disabled)
    private val overrideFile: File
) : QuickPhrase() {

    init {
        ensureFileExists()
    }

    var override: CustomQuickPhrase? =
        if (overrideFile.exists())
            CustomQuickPhrase(overrideFile)
        else {
            val disabledOverride = File(overrideFile.path + ".$DISABLE")
            if (disabledOverride.exists())
                CustomQuickPhrase(disabledOverride)
            else
                null
        }
        private set

    override val isEnabled: Boolean
        get() = override?.isEnabled ?: true

    private fun createOverrideIfNotExist() {
        if (override != null)
            return
        file.copyTo(overrideFile, overwrite = true)
        override = CustomQuickPhrase(overrideFile)
    }

    private fun loadBuiltinData() = QuickPhraseData.fromLines(file.readLines())

    override fun loadData(): Result<QuickPhraseData> =
        if (override == null)
            loadBuiltinData()
        else
            override!!.loadData()


    override fun saveData(data: QuickPhraseData) {
        createOverrideIfNotExist()
        override!!.saveData(data)
    }

    override fun enable() {
        if (isEnabled)
            return
        // override must exist in this case
        override!!.enable()
    }

    override fun disable() {
        if (!isEnabled)
            return
        createOverrideIfNotExist()
        override!!.disable()
    }

    fun deleteOverride() {
        overrideFile.delete()
        override = null
    }

    override fun toString(): String {
        return "BuiltinQuickPhrase(file=$file, overrideFile=$overrideFile, override=$override, isEnabled=$isEnabled)"
    }

}
