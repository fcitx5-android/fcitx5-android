package me.rocka.fcitx5test.settings.parsed

import cn.berberman.girls.utils.either.*
import me.rocka.fcitx5test.native.RawConfig

sealed class ConfigDescriptor<T, U>(
    val name: String,
    val type: ConfigType<T>,
    val description: String? = null,
    val defaultValue: U? = null
) {

    class ConfigTopLevelDef(
        val name: String,
        val values: List<ConfigDescriptor<*, *>>,
        val customTypes: List<ConfigCustomTypeDef>
    )

    class ConfigCustomTypeDef(
        val name: String,
        val values: List<ConfigDescriptor<*, *>>
    )

    class ConfigInt(
        name: String,
        description: String? = null,
        defaultValue: Int? = null,
        val intMax: Int?,
        val intMin: Int?,
    ) : ConfigDescriptor<ConfigType.TyInt, Int>(
        name, ConfigType.TyInt, description, defaultValue
    )

    class ConfigString(
        name: String,
        description: String? = null,
        defaultValue: String? = null,
    ) : ConfigDescriptor<ConfigType.TyString, String>(
        name, ConfigType.TyString, description, defaultValue
    )

    class ConfigBool(
        name: String,
        description: String? = null,
        defaultValue: Boolean? = null,
    ) : ConfigDescriptor<ConfigType.TyBool, Boolean>(
        name, ConfigType.TyBool, description, defaultValue
    )

    class ConfigKey(
        name: String,
        description: String? = null,
        defaultValue: String? = null,
    ) : ConfigDescriptor<ConfigType.TyKey, String>(
        name, ConfigType.TyKey, description, defaultValue
    )

    class ConfigEnum(
        name: String,
        description: String? = null,
        defaultValue: String? = null,
        val entries: List<String>,
        val entriesI18n: List<String>
    ) : ConfigDescriptor<ConfigType.TyEnum, String>(
        name, ConfigType.TyEnum, description, defaultValue
    )

    class ConfigCustom(
        name: String,
        type: ConfigType.TyCustom,
        description: String? = null,
        // will be filled in parseTopLevel
        var children: ConfigCustomTypeDef? = null
    ) : ConfigDescriptor<ConfigType.TyCustom, Nothing>(
        name, type, description, null
    )

    class ConfigList(
        name: String,
        type: ConfigType.TyList,
        description: String? = null,
        defaultValue: List<Any?>? = null,
    ) : ConfigDescriptor<ConfigType.TyList, List<Any?>>(
        name, type, description, defaultValue
    )

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
            class NoTypeExist(val config: RawConfig) : ParseException()
            class TypeNoParse(val sup: ConfigType.Companion.UnknownConfigTypeException) :
                ParseException()

            class NoEnumFound(val config: RawConfig) : ParseException()
            class NoEnumI18nFound(val config: RawConfig) : ParseException()
            class BadFormList(val type: ConfigType<*>) : ParseException()
            class BadFormDesc(val config: RawConfig) : ParseException()
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
                            val entries = raw.enum ?: throw  ParseException.NoEnumFound(raw)
                            val entriesI18n =
                                raw.enumI18n ?: throw  ParseException.NoEnumI18nFound(raw)
                            ConfigEnum(
                                raw.name,
                                raw.description,
                                raw.defaultValue,
                                entries,
                                entriesI18n
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
                        is ConfigType.TyList -> ConfigList(
                            raw.name,
                            it,
                            raw.description,
                            raw.findByName("DefaultValue")?.subItems?.map { ele ->
                                when (it.subtype) {
                                    ConfigType.TyBool -> ele.value.toBoolean()
                                    ConfigType.TyInt -> ele.value.toInt()
                                    ConfigType.TyKey -> ele.value
                                    ConfigType.TyString -> ele.value
                                    else -> throw  ParseException.BadFormList(it)
                                }
                            }
                        )
                        ConfigType.TyString -> ConfigString(
                            raw.name,
                            raw.description,
                            raw.defaultValue
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
                    parsed.children = customTypeDef.find { cTy ->
                        cTy.name == (parsed.type as ConfigType.TyCustom).typeName
                    }
                parsed
            } ?: listOf()
            return ConfigTopLevelDef(topLevel.name, topDesc, customTypeDef)
        }

        fun parseTopLevel(raw: RawConfig): Either<ParseException, ConfigTopLevelDef> {
            return parseTopLevelE(raw).runCatchingEither { this }

        }
    }
}