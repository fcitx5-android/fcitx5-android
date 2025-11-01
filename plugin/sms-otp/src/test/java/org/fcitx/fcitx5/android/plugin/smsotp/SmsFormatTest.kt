package org.fcitx.fcitx5.android.plugin.smsotp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmSFormatTest {
    @Test
    fun testExtractOtp_basic() {
        assertEquals("123456", extractOtp("Your OTP is 123456."))
        assertEquals("987654", extractOtp("验证码：987654"))
        assertEquals("1234", extractOtp("Code: 1234"))
        assertEquals("12345678", extractOtp("OTP: 12345678"))
        assertEquals("123456", extractOtp("123-456"))
    }

    @Test
    fun testExtractOtp_withNoise() {
        assertEquals("123456", extractOtp("abc 123456 def"))
        assertNull(extractOtp("abc.1234567/def"))
        assertNull(extractOtp("2004/10/12, 12:00:00"))
    }

    @Test
    fun testExtractOtp_invalid() {
        assertNull(extractOtp(null))
        assertNull(extractOtp("No code here"))
        assertNull(extractOtp("123")) // too short
        assertNull(extractOtp("123456789")) // too long
    }

    @Test
    fun testExtractOtp_multipleCodes() {
        assertEquals("123456", extractOtp("First: 123456, Second: 654321"))
    }

    @Test
    fun testExtractOtp_realSamples() {
        assertEquals("401572", extractOtp("WhatsApp code 401-572"))
        assertEquals("28843", extractOtp("28843 e o codigo de confirmacao do Facebook de Pedro Davi #fb"))
        assertEquals("932033", extractOtp("Your DENT code is: 932033"))
        assertEquals("050475", extractOtp("Ваш код перевірки Poe: 050475. Не повідомляйте цей код іншим."))
        assertEquals("429309", extractOtp("[抖音] 429309 is your verification code, valid for 5 minutes."))
        assertEquals("8650", extractOtp("<#> 8650 is your Venmo phone verification code."))
        assertEquals("060973", extractOtp("12306用户注册或既有用户手机核验专用验证码：060973。如非本人直接访问12306，请停止操作，切勿将验证码提供给第三方。【铁路客服】"))
        assertEquals("505513", extractOtp("您在付款，为防诈骗千万不要告诉他人验证码505513，商户为汇付天下，金额80元。如有疑问请停止操作。（短信编号：245747）【工商银行】"))
        assertEquals("370000", extractOtp("任何向你索要验证码的都是骗子，千万别给！您正在向www（尾号4832）转账，验证码370000，100元。"))
        assertEquals("927711", extractOtp("TAN to confirm mandate for Segpay UK Test Please enter TAN 927711 to set up your mandate 10751532bd3573241262f56b4e76a003 securely."))
        assertEquals("121093", extractOtp("Twój kod do Tindera to 121093 Nie udostępniaj @tinder.com #121093"))
        assertNull(extractOtp("OKX：USDTsuccessfully withdrawn: 132,619.89 【okxcash.com】Account: DTm888 Key: Swksf367"))
        assertNull(extractOtp("You can also tap on this link to verify your phone: v.whatsapp.com/696948"))
        assertNull(extractOtp("https://www.photosfromyourevent.com/4669/wauem8/"))
    }

}
