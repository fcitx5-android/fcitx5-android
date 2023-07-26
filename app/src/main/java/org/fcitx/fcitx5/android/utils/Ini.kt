package org.fcitx.fcitx5.android.utils

import cc.ekblad.konbini.*
import org.intellij.lang.annotations.Language

typealias IniProperties = MutableList<Ini.Annotated<Ini.Property>>

fun IniProperties.getValue(key: String): String? {
    val prop = find { it.data.name == key } ?: return null
    return prop.data.value
}

fun IniProperties.setValue(key: String, newValue: String) {
    val idx = indexOfFirst { it.data.name == key }
    val prop = Ini.Property(key, newValue)
    if (idx >= 0) {
        set(idx, Ini.Annotated(get(idx).comments, prop))
    } else {
        add(Ini.Annotated(prop))
    }
}

data class Ini(
    val properties: IniProperties,
    val sections: MutableMap<String, Annotated<IniProperties>>,
    val trailingComments: MutableList<String>
) {

    data class Property(var name: String, var value: String)

    data class Annotated<T>(
        val comments: MutableList<String>,
        val data: T
    ) {
        constructor(data: T) : this(mutableListOf(), data)
    }

    fun getValue(key: String) = properties.getValue(key)

    fun getValue(section: String, key: String) = sections[section]?.data?.getValue(key)

    fun setValue(key: String, newValue: String) = properties.setValue(key, newValue)

    fun setValue(section: String, key: String, newValue: String) {
        val s = sections[section] ?: Annotated(mutableListOf<Annotated<Property>>()).also {
            sections[section] = it
        }
        s.data.setValue(key, newValue)
    }

}

object IniPrettyPrinter {
    data class PrettyOptions(
        val separator: Separator,
        val commentStart: CommentStart,
        val spaceAroundSeparator: Boolean
    ) {
        enum class Separator {
            Colon, Equal
        }

        enum class CommentStart {
            Semicolon, Pound
        }
    }

    private val defaultPrettyOptions = PrettyOptions(
        PrettyOptions.Separator.Equal,
        PrettyOptions.CommentStart.Semicolon,
        false
    )

    fun pretty(ini: Ini, prettyOptions: PrettyOptions = defaultPrettyOptions): String {
        val sb = StringBuilder()
        fun prettyProperty(property: Ini.Property) {
            sb.append(property.name)
            if (prettyOptions.spaceAroundSeparator) {
                sb.append(' ')
            }
            when (prettyOptions.separator) {
                PrettyOptions.Separator.Colon -> sb.append(':')
                PrettyOptions.Separator.Equal -> sb.append('=')
            }
            if (prettyOptions.spaceAroundSeparator) {
                sb.append(' ')
            }
            sb.appendLine(property.value)
        }

        fun prettyComments(comments: List<String>) {
            comments.forEach {
                when (prettyOptions.commentStart) {
                    PrettyOptions.CommentStart.Semicolon -> sb.append(';')
                    PrettyOptions.CommentStart.Pound -> sb.append('#')
                }
                sb.appendLine(it)
            }
        }
        ini.properties.forEach {
            prettyComments(it.comments)
            prettyProperty(it.data)
        }
        if (ini.properties.isNotEmpty()) {
            sb.appendLine()
        }
        ini.sections.forEach { (n, properties) ->
            prettyComments(properties.comments)
            sb.appendLine("[$n]")
            properties.data.forEach {
                prettyComments(it.comments)
                prettyProperty(it.data)
            }
            sb.appendLine()
        }
        prettyComments(ini.trailingComments)
        return sb.toString()
    }
}

object IniParser {
    private inline fun <T> lexeme(crossinline parser: Parser<T>) = parser {
        val data = parser()
        whitespace()
        data
    }

    @Language("RegExp")
    private val name = lexeme(regex("[a-zA-Z0-9._/\\[\\]]+"))

    @Language("RegExp")
    private val sectionName = lexeme(regex("[a-zA-Z0-9._/]+"))

    private val value = parser {
        @Language("RegExp")
        val data = regex("[^\n]*")
        whitespace()
        data.trim()
    }

    private val hash = char('#')
    private val semi = char(';')
    private val comment = parser {
        oneOf(hash, semi)
        @Language("RegExp")
        val data = regex("[^\n]*")
        whitespace()
        data
    }
    private val colon = char(':')
    private val equal = char('=')
    private val bOpen = parser { whitespace(); char('[') }
    private val bClose = parser { char(']'); whitespace() }

    private inline fun <T> annotated(crossinline parser: Parser<T>) = parser {
        val comments = many(comment)
        val data = parser()
        Ini.Annotated(comments.toMutableList(), data)
    }

    private val property = annotated(parser {
        val n = name()
        oneOf(colon, equal)
        val v = value()
        Ini.Property(n, v)
    })

    private val section = annotated(parser {
        val n = bracket(bOpen, bClose, sectionName)
        val properties = many(property)
        n to properties
    })

    private val ini = parser {
        val globals: IniProperties = mutableListOf()
        val sections = mutableMapOf<String, Ini.Annotated<IniProperties>>()
        whitespace()
        many {
            oneOf(
                section.map {
                    sections[it.data.first] =
                        Ini.Annotated(it.comments, it.data.second.toMutableList())
                },
                property.map { globals.add(it) }
            )
        }
        val trailingComments = annotated(whitespace).map { it.comments }()
        Ini(globals, sections, trailingComments)
    }

    fun parse(text: String) = ini.parseToEnd(text)
}