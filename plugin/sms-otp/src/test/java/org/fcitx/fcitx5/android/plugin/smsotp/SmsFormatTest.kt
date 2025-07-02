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
        assertNull(extractOtp("OKX：USDTsuccessfully withdrawn: 132,619.89 【okxcash.com】Account: DTm888 Key: Swksf367"))
        assertNull(extractOtp("You can also tap on this link to verify your phone: v.whatsapp.com/696948"))
        assertNull(extractOtp("https://www.photosfromyourevent.com/4669/wauem8/"))
    }
}
