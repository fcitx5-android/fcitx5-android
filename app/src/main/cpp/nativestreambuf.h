#ifndef FCITX5_ANDROID_NATIVESTREAMBUF_H
#define FCITX5_ANDROID_NATIVESTREAMBUF_H

#include <array>
#include <iostream>

template<class char_type>
using streambuf_callback_t = void (*)(const char_type *);

template<std::size_t SIZE = 128, class CharT = char>
class native_streambuf : public std::basic_streambuf<CharT>, public std::ostream {
public:
    using Base = std::basic_streambuf<CharT>;
    using char_type = typename Base::char_type;
    using int_type = typename Base::int_type;
    using callback_t = streambuf_callback_t<CharT>;

    native_streambuf() : _buffer{}{
        Base::setp(_buffer.begin(), _buffer.end() - 1);
    }

    int_type overflow(int_type ch) override {
        sync();
        return 0;
    }

    int_type sync() override {
        *Base::pptr() = '\0';
        _callback(Base::pbase());
        Base::setp(_buffer.begin(), _buffer.end() - 1);
        return 0;
    }

    void set_callback(callback_t callback) {
        _callback = callback;
    }

private:
    std::array<char_type, SIZE> _buffer;
    callback_t _callback = nullptr;
};

#endif //FCITX5_ANDROID_NATIVESTREAMBUF_H
