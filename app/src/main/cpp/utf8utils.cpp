#include "fcitx-utils/utf8.h"

extern "C" bool validateUTF8(const char *s) {
    std::string str = s;
    return fcitx::utf8::validate(str);
}