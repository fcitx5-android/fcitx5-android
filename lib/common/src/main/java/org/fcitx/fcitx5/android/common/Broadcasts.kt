package org.fcitx.fcitx5.android.common

@Suppress("PropertyName")
class Broadcasts(applicationId: String) {
    val FcitxInputMethodServiceCreated = "$applicationId.IME_SERVICE_CREATED"
    val FcitxInputMethodServiceDestroyed = "$applicationId.IME_SERVICE_DESTROYED"
    val FcitxApplicationCreated = "$applicationId.android.APP_CREATED"
    val PERMISSION = "$applicationId.android.permission.BROADCAST"
}
