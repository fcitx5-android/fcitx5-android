package me.rocka.fcitx5test.keyboard.layout

import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import me.rocka.fcitx5test.R

class CustomKeyboardView(val root: ViewGroup) {
    val candidateList: RecyclerView
        get() = root.findViewById(R.id.candidate_list)
    val caps: ImageButton
        get() = root.findViewById(R.id.button_caps)
    val backspace: ImageButton
        get() = root.findViewById(R.id.button_backspace)
    val quickphrase: ImageButton
        get() = root.findViewById(R.id.button_quickphrase)
    val lang: ImageButton
        get() = root.findViewById(R.id.button_lang)
    val space: Button
        get() = root.findViewById(R.id.button_space)
    val punctuation: Button
        get() = root.findViewById(R.id.button_lang)
    val `return`: Button
        get() = root.findViewById(R.id.button_return)
}
