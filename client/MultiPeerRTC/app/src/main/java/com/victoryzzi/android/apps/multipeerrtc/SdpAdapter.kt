package com.victoryzzi.android.apps.multipeerrtc

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class SdpAdapter : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {
        loge("onCreateSuccess : ${p0?.type ?: "p0 == null"}")
    }

    override fun onSetSuccess() {
        loge("onSetSuccess")
    }

    override fun onCreateFailure(p0: String?) {
        loge("onCreateFailure : ${p0 ?: "p0 == null"}")
    }

    override fun onSetFailure(p0: String?) {
        loge("onSetFailure : ${p0 ?: "p0 == null"}")
    }
}