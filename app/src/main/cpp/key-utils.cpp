#include "fcitx-utils/key.h"

struct parsed_key {
    bool successful;
    const char *str;
    uint32_t sym;
    uint32_t states;
};


extern "C" parsed_key parse_key(const char *raw) {
    fcitx::Key key(raw);
    return parsed_key{
            key.sym() != FcitxKey_None,
            key.toString().c_str(),
            key.sym(),
            key.states()
    };
}