package me.rocka.fcitx5test

import androidx.appcompat.app.AppCompatActivity
import com.google.gson.annotations.SerializedName
import java.io.File

data class VKconfig (
    @SerializedName("imes") var imes : List<Ime>,
    @SerializedName("vKeyboards") var vKeyboards : List<VKeyboard>
)

data class Ime (
    @SerializedName("name") var name : String,
    @SerializedName("vKeyboard") var vKeyboard : String
)

data class Press (
    @SerializedName("text") var text : String = "",
    @SerializedName("type") var type : String = "doNothing"
)

data class Key (

    @SerializedName("name") var name : String = "",
    @SerializedName("display") var display : String = "#",
    @SerializedName("short") var short : Press = Press(),
    @SerializedName("long") var long : Press = Press(),
    @SerializedName("color") var color : String = ""

)

data class VKeyboard (
    @SerializedName("name") var name : String,
    @SerializedName("layout") var layout : String,
    @SerializedName("keys") var keys : List<Key>
)