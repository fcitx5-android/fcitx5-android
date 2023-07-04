package org.fcitx.fcitx5.android.common

@Suppress("PropertyName")
class Broadcasts(applicationId: String) {
    @JvmField
    val FcitxInputMethodServiceCreated = "$applicationId.IME_SERVICE_CREATED"

    @JvmField
    val FcitxInputMethodServiceDestroyed = "$applicationId.IME_SERVICE_DESTROYED"

    @JvmField
    val FcitxApplicationCreated = "$applicationId.APP_CREATED"

    @JvmField
    val PERMISSION = "$applicationId.permission.BROADCAST"
}
