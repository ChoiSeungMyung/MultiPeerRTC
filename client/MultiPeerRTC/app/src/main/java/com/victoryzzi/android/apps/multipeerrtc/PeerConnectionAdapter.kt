package com.victoryzzi.android.apps.multipeerrtc

import org.webrtc.*

open class PeerConnectionAdapter : PeerConnection.Observer {
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        loge("onSiganlingChange : ${p0?.name ?: "p0 == null"}")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        loge("onIceConnectionChange : ${p0?.name ?: "p0 == null"}")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        loge("onIceGatheringChange : $p0")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        loge("onIceGatheringChange : ${p0?.name ?: "p0 == null"}")
    }

    override fun onIceCandidate(p0: IceCandidate?) {
        loge("onIceCandidate : ${p0?.serverUrl ?: "p0 == null"}")
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        loge("onIceCandidatesRemoved : ${p0?.isEmpty() ?: "p0 == null"}")
    }

    override fun onAddStream(p0: MediaStream?) {
        loge("onAddStream : ${p0?.id ?: "p0 == null"}")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        loge("onRemoveStream : ${p0?.id ?: "p0 == null"}")
    }

    override fun onDataChannel(p0: DataChannel?) {
        loge("onDataChannel : ${p0?.id() ?: "p0 == null"}")
    }

    override fun onRenegotiationNeeded() {
        loge("onRenegotiationNeeded")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        loge("onAddTrack : ${p0?.id() ?: "p0 == null"}, ${p1?.size ?: "p1Array is null"}")
    }
}