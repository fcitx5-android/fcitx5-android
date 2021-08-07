#ifndef FCITX5TEST_ANDROIDSTREAMBUF_H
#define FCITX5TEST_ANDROIDSTREAMBUF_H

#include <streambuf>
#include <fcitx-utils/stringutils.h>
#include <android/log.h>

class AndroidStreamBuf : public std::streambuf {
public:
    AndroidStreamBuf(const char *tag, size_t buf_size) : tag_(tag), buf_size_(buf_size) {
        assert(buf_size_ > 0);
        pbuf_ = new char[buf_size_];
        memset(pbuf_, 0, buf_size_);

        setp(pbuf_, pbuf_ + buf_size_);
    }

    ~AndroidStreamBuf() override { delete pbuf_; }

    int overflow(int c) override {
        if (-1 == sync()) {
            return traits_type::eof();
        } else {
            // put c into buffer after successful sync
            if (!traits_type::eq_int_type(c, traits_type::eof())) {
                sputc(traits_type::to_char_type(c));
            }

            return traits_type::not_eof(c);
        }
    }

    int sync() override {
        auto str_buf = fcitx::stringutils::trim(std::string(pbuf_));
        auto trim_pbuf = str_buf.c_str();

        int res = __android_log_write(ANDROID_LOG_DEBUG, tag_, trim_pbuf);

        memset(pbuf_, 0, buf_size_);
        setp(pbase(), pbase() + buf_size_);
        pbump(0);
        return res;
    }

private:
    const char *tag_;
    const size_t buf_size_;
    char *pbuf_;
};

#endif //FCITX5TEST_ANDROIDSTREAMBUF_H
