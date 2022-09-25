#ifndef FCITX5_ANDROID_NATIVESTREAMBUF_H
#define FCITX5_ANDROID_NATIVESTREAMBUF_H

#include <array>
#include <iostream>

#include <android/log.h>

template<std::size_t SIZE = 128, class CharT = char>
class native_streambuf : public std::basic_streambuf<CharT>, public std::ostream {
public:
    using Base = std::basic_streambuf<CharT>;
    using char_type = typename Base::char_type;
    using int_type = typename Base::int_type;

    native_streambuf() : _buffer{} {
        Base::setp(_buffer.begin(), _buffer.end() - 1);
    }

    int_type overflow(int_type ch) override {
        sync();
        return 0;
    }

    int_type sync() override {
        *Base::pptr() = '\0';
        const char *text = Base::pbase();
        android_LogPriority prio = ANDROID_LOG_DEBUG;
        switch (text[0]) {
            case 'I':
                prio = ANDROID_LOG_INFO;
                break;
            case 'W':
                prio = ANDROID_LOG_WARN;
                break;
            case 'E':
                prio = ANDROID_LOG_ERROR;
                break;
            case 'F':
                prio = ANDROID_LOG_FATAL;
                break;
        }
        __android_log_write(prio, "fcitx5", text + 1);
        Base::setp(_buffer.begin(), _buffer.end() - 1);
        return 0;
    }

private:
    std::array<char_type, SIZE> _buffer;
};

#endif //FCITX5_ANDROID_NATIVESTREAMBUF_H
