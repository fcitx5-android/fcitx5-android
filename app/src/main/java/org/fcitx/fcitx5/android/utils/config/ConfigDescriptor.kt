package org.fcitx.fcitx5.android.utils.config

import cn.berberman.girls.utils.either.*
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.utils.MyParser
import java.io.Serializable

sealed class ConfigDescriptor<T, U> : Serializable {
    abstract val name: String
    abstract val type: ConfigType<T>
    abstract val description: String?
    abstract val defaultValue: U?

    data class ConfigTopLevelDef(
        val name: String,
        val values: List<ConfigDescriptor<*, *>>,
        val customTypes: List<ConfigCustomTypeDef>
    ) : Serializable

    data class ConfigCustomTypeDef(
        val name: String,
        val values: List<ConfigDescriptor<*, *>>
    ) : Serializable

    data class ConfigInt(
        override val name: String,
        override val description: String? = null,
        override val defaultValue: Int? = null,
        val intMax: Int?,
        val intMin: Int?,
    ) : ConfigDescriptor<ConfigType.TyInt, Int>() {
        override val type: ConfigType<ConfigType.TyInt>
            get() = ConfigType.TyInt
    }

    data class ConfigString(
        override val name: String,
        override val description: String? = null,
        override val defaultValue: String? = null,
    ) : ConfigDescriptor<ConfigType.TyString, String>() {
        override val type: ConfigType<ConfigType.TyString>
            get() = ConfigType.TyString
    }

    data class ConfigBool(
        override val name: String,
        override val description: String? = null,
        override val defaultValue: Boolean? = null,
    ) : ConfigDescriptor<ConfigType.TyBool, Boolean>() {
        override val type: ConfigType<ConfigType.TyBool>
            get() = ConfigType.TyBool

    }

    // TODO: Placeholder
    data class ConfigKey(
        override val name: String,
        override val description: String? = null,
        override val defaultValue: String? = null,
    ) : ConfigDescriptor<ConfigType.TyKey, String>() {
        override val type: ConfigType<ConfigType.TyKey>
            get() = ConfigType.TyKey

    }

    data class ConfigEnum(
        override val name: String,
        override val description: String? = null,
        override val defaultValue: String? = null,
        val entries: List<String>,
        val entriesI18n: List<String>?
    ) : ConfigDescriptor<ConfigType.TyEnum, String>() {
        override val type: ConfigType<ConfigType.TyEnum>
            get() = ConfigType.TyEnum

    }

    data class ConfigCustom(
        override val name: String,
        override val type: ConfigType.TyCustom,
        override val description: String? = null,
        // will be filled in parseTopLevel
        var customTypeDef: ConfigCustomTypeDef? = null
    ) : ConfigDescriptor<ConfigType.TyCustom, Nothing>() {
        override val defaultValue: Nothing?
            get() = null
    }

    data class ConfigList(
        override val name: String,
        override val type: ConfigType.TyList,
        override val description: String? = null,
        /**
         * [Any?] is used for a union type. See [parseE] for details.
         */
        override val defaultValue: List<Any?>? = null,
    ) : ConfigDescriptor<ConfigType.TyList, List<Any?>>()

    /**
     * Specialized [ConfigList] for enum
     */
    data class ConfigEnumList(
        override val name: String,
        override val description: String? = null,
        override val defaultValue: List<String>? = null,
        val entries: List<String>,
        val entriesI18n: List<String>?
    ) :
        ConfigDescriptor<ConfigType.TyList, List<String>>() {
        override val type: ConfigType<ConfigType.TyList>
            get() = ConfigType.TyList(ConfigType.TyEnum)
    }

    // TODO: Placeholder
    data class ConfigExternal(
        override val name: String,
        override val description: String? = null,
    ) : ConfigDescriptor<ConfigType.TyExternal, Nothing>() {
        override val type: ConfigType<ConfigType.TyExternal>
            get() = ConfigType.TyExternal
        override val defaultValue: Nothing?
            get() = null
    }

    companion object :
        MyParser<RawConfig, ConfigDescriptor<*, *>, Companion.ParseException> {

        private val RawConfig.type
            get() = findByName("Type")?.value?.let { ConfigType.parse(it) }
        private val RawConfig.description
            get() = findByName("Description")?.value
        private val RawConfig.defaultValue
            get() = findByName("DefaultValue")?.value
        private val RawConfig.enum
            get() = findByName("Enum")?.subItems?.map { it.value }
        private val RawConfig.enumI18n
            get() = findByName("EnumI18n")?.subItems?.map { it.value }
        private val RawConfig.intMin
            get() = findByName("IntMin")?.value?.toInt()
        private val RawConfig.intMax
            get() = findByName("IntMax")?.value?.toInt()

        sealed class ParseException : Exception() {
            data class NoTypeExist(val config: RawConfig) : ParseException()
            data class TypeNoParse(val sup: ConfigType.Companion.UnknownConfigTypeException) :
                ParseException()

            data class NoEnumFound(val config: RawConfig) : ParseException()
            data class BadFormList(val type: ConfigType<*>) : ParseException()
            data class BadFormDesc(val config: RawConfig) : ParseException()
        }

        private fun parseE(raw: RawConfig): ConfigDescriptor<*, *> {
            val type = raw.type ?: throw ParseException.NoTypeExist(raw)
            return type.patternMatching<ConfigType.Companion.UnknownConfigTypeException, ConfigType<*>, ConfigDescriptor<*, *>>()
                .onLeft { throw ParseException.TypeNoParse(it) }
                .onRight {
                    when (it) {
                        ConfigType.TyBool ->
                            ConfigBool(
                                raw.name,
                                raw.description,
                                raw.defaultValue?.toBoolean()

                            )
                        is ConfigType.TyCustom -> ConfigCustom(
                            raw.name,
                            it,
                            raw.description
                        )
                        ConfigType.TyEnum -> {
                            val entries = raw.enum ?: throw ParseException.NoEnumFound(raw)
                            ConfigEnum(
                                raw.name,
                                raw.description,
                                raw.defaultValue,
                                entries,
                                raw.enumI18n
                            )
                        }
                        ConfigType.TyInt -> ConfigInt(
                            raw.name,
                            raw.description,
                            raw.defaultValue?.toInt(),
                            raw.intMax,
                            raw.intMin
                        )
                        ConfigType.TyKey -> ConfigKey(raw.name, raw.description, raw.defaultValue)
                        is ConfigType.TyList ->
                            if (it.subtype == ConfigType.TyEnum) {
                                val entries = raw.enum ?: throw ParseException.NoEnumFound(raw)
                                ConfigEnumList(
                                    raw.name,
                                    raw.description,
                                    raw.findByName("DefaultValue")?.subItems?.map { ele -> ele.value },
                                    entries,
                                    raw.enumI18n
                                )
                            } else
                                ConfigList(
                                    raw.name,
                                    it,
                                    raw.description,
                                    raw.findByName("DefaultValue")?.subItems?.map { ele ->
                                        when (it.subtype) {
                                            ConfigType.TyBool -> ele.value.toBoolean()
                                            ConfigType.TyInt -> ele.value.toInt()
                                            ConfigType.TyKey -> ele.value
                                            ConfigType.TyString -> ele.value
                                            ConfigType.TyEnum -> throw IllegalAccessException("Impossible!")
                                            else -> throw ParseException.BadFormList(it)
                                        }
                                    }
                                )
                        ConfigType.TyString -> ConfigString(
                            raw.name,
                            raw.description,
                            raw.defaultValue
                        )
                        ConfigType.TyExternal -> ConfigExternal(
                            raw.name,
                            raw.description
                        )
                    }
                }
                .eval()

        }

        override fun parse(raw: RawConfig): Either<ParseException, ConfigDescriptor<*, *>> {
            return parseE(raw).runCatchingEither { this }
        }

        private fun parseTopLevelE(raw: RawConfig): ConfigTopLevelDef {
            val topLevel = raw.subItems?.get(0) ?: throw ParseException.BadFormDesc(raw)
            val customTypeDef = raw.subItems?.drop(1)?.mapNotNull {
                it.subItems?.map { ele -> parseE(ele) }
                    ?.let { parsed -> ConfigCustomTypeDef(it.name, parsed) }
            } ?: listOf()
            val topDesc = topLevel.subItems?.map {
                val parsed = parseE(it)
                if (parsed is ConfigCustom)
                    parsed.customTypeDef = customTypeDef.find { cTy ->
                        cTy.name == parsed.type.typeName
                    }
                parsed
            } ?: listOf()
            return ConfigTopLevelDef(topLevel.name, topDesc, customTypeDef)
        }

        fun parseTopLevel(raw: RawConfig): Either<ParseException, ConfigTopLevelDef> =
            runCatchingEither {
                parseTopLevelE(raw)
            }
    }
}