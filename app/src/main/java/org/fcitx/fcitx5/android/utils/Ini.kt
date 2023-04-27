package org.fcitx.fcitx5.android.utils

import cc.ekblad.konbini.*
import org.intellij.lang.annotations.Language


data class Ini(
    val properties: MutableList<Annotated<Property>>,
    val sections: MutableMap<String, Annotated<MutableList<Annotated<Property>>>>,
    val trailingComments: MutableList<String>
) {
    data class Property(var name: String, var value: String)

    data class Annotated<T>(
        val comments: MutableList<String>,
        val data: T
    ) {
        constructor(data: T) : this(mutableListOf(), data)
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
            if (prettyOptions.spaceAroundSeparator)
                sb.append(' ')
            when (prettyOptions.separator) {
                PrettyOptions.Separator.Colon -> sb.append(':')
                PrettyOptions.Separator.Equal -> sb.append('=')
            }
            if (prettyOptions.spaceAroundSeparator)

                sb.append(' ')
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
        ini.sections.forEach { (n, properties) ->
            prettyComments(properties.comments)
            sb.appendLine("[$n]")
            properties.data.forEach {
                prettyComments(it.comments)
                prettyProperty(it.data)
            }
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

    @Language("RegExp")
    private val value = lexeme(regex(".+"))

    @Language("RegExp")
    private val comment = lexeme(regex("[;#].*"))
    private val colon = lexeme(char(':'))
    private val equal = lexeme(char('='))
    private val bOpen = lexeme(char('['))
    private val bClose = lexeme(char(']'))

    private inline fun <T> annotated(crossinline parser: Parser<T>) = parser {
        val comments = many(comment).map { it.drop(1) }
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
        val globals = mutableListOf<Ini.Annotated<Ini.Property>>()
        val sections =
            mutableMapOf<String, Ini.Annotated<MutableList<Ini.Annotated<Ini.Property>>>>()
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