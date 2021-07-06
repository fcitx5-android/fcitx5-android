set(Gettext_FOUND TRUE)
set(GETTEXT_VERSION_STRING 0.0.0)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Gettext
    FOUND_VAR
        Gettext_FOUND
    REQUIRED_VARS
        GETTEXT_VERSION_STRING
)

mark_as_advanced(Gettext_FOUND GETTEXT_VERSION_STRING)
