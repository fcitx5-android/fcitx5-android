package me.rocka.fcitx5test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.rocka.fcitx5test.ui.olist.BaseOrderedListUi
import splitties.views.dsl.core.setContentView

// TODO: debug
class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(object :
            BaseOrderedListUi<String>(this, Mode.FreeAddString(), listOf("A", "B", "C"), false) {
            override fun showEntry(x: String): String = x
        })
    }
}