find_package(Gettext)

# Do as I say, not as I do
# https://crascit.com/2018/09/14/do-not-redefine-cmake-commands/
if (NOT COMMAND _add_custom_command)
    function(add_custom_command)
        cmake_parse_arguments(arg "" "" "COMMAND" ${ARGN})
        list(GET arg_COMMAND 0 cmd_0)
        # find calls in fcitx5_install_translation
        if (cmd_0 STREQUAL GETTEXT_MSGFMT_EXECUTABLE AND NOT "-d" IN_LIST arg_COMMAND)
            # ARGV is a list like: "COMMAND;/path/to/msgfmt;-o;mo_file;po_file"
            # insert arguments after GETTEXT_MSGFMT_EXECUTABLE
            list(FIND ARGV "${GETTEXT_MSGFMT_EXECUTABLE}" idx)
            math(EXPR idx "${idx} + 1")
            list(INSERT ARGV ${idx} "--no-hash" "--endianness=little")
        endif ()
        _add_custom_command(${ARGV})
    endfunction()
endif ()
