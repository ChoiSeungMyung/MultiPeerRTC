package com.victoryzzi.android.apps.multipeerrtc

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection


class MainActivity : AppCompatActivity(), SignalingClient.Callback {
    lateinit var mediaStream: MediaStream
    lateinit var peerConnectionFactory: PeerConnectionFactory
    lateinit var remoteViews: Array<SurfaceViewRenderer>

    val eglBaseContext = EglBase.create().eglBaseContext
    val iceServers: ArrayList<PeerConnection.IceServer> = ArrayList()
    val peerConnectionMap: HashMap<String, PeerConnection> = HashMap()
    var remoteViewsIndex = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())

        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions())
        val options = PeerConnectionFactory.Options()

        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBaseContext)

        peerConnectionFactory =
            PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory()

        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
        val videoCapturer = createCameraCapturer(true)
        val videoSource = peerConnectionFactory.createVideoSource(
            videoCapturer?.isScreencast ?: throw IllegalStateException("videoCapturer is null")
        )

        videoCapturer.apply {
            initialize(surfaceTextureHelper, applicationContext, videoSource.capturerObserver)
            startCapture(480, 640, 30) //TODO : 해상도, fps 설정
        }

        localView.apply {
            setMirror(true)
            init(eglBaseContext, null)
        }

        val videoTrack =
            peerConnectionFactory.createVideoTrack("100", videoSource).apply { addSink(localView) }

        remoteViews = arrayOf(remoteView, remoteView2, remoteView3)

        remoteViews.forEach { remoteView ->
            remoteView.setMirror(true)
            remoteView.init(eglBaseContext, null)
        }

        mediaStream = peerConnectionFactory.createLocalMediaStream("mediaStream")
            .apply {
                addTrack(videoTrack)
            }

        SignalingClient.get()?.init(this)
    }

    @Synchronized
    private fun getOrCreatePeerConnection(socketId: String?): PeerConnection {
        var peerConnection = peerConnectionMap[socketId]

        peerConnection?.let { return it }

        peerConnection = peerConnectionFactory.createPeerConnection(
            iceServers,
            object : PeerConnectionAdapter() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
                    SignalingClient.get()!!.sendIceCandidate(p0!!, socketId)
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    val remoteVideoTrack = p0!!.videoTracks[0]
                    runOnUiThread {
                        remoteVideoTrack.addSink(
                            remoteViews[remoteViewsIndex++]
                        )
                    }
                }
            }) ?: throw IllegalArgumentException("not init peerConnection")
        peerConnection.addStream(mediaStream)
        peerConnectionMap[socketId!!] = peerConnection

        return peerConnection
    }

    override fun onCreateRoom() {

    }

    override fun onPeerJoined(socketId: String?) {
        val peerConnection = getOrCreatePeerConnection(socketId)
        peerConnection.createOffer(object : SdpAdapter() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                super.onCreateSuccess(p0)
                peerConnection.setLocalDescription(SdpAdapter(), p0)
                SignalingClient.get()?.sendSessionDescription(p0!!, socketId)
            }
        }, MediaConstraints())
    }

    override fun onSelfJoined() {

    }

    override fun onPeerLeave(msg: String?) {

    }

    override fun onOfferReceived(data: JSONObject?) {
        runOnUiThread {
            val socketId = data?.optString("from")

            val peerConnection = getOrCreatePeerConnection(socketId)

            peerConnection.setRemoteDescription(
                SdpAdapter(),
                SessionDescription(SessionDescription.Type.OFFER, data?.optString("sdp"))
            )

            peerConnection.createAnswer(object : SdpAdapter() {
                override fun onCreateSuccess(p0: SessionDescription?) {
                    super.onCreateSuccess(p0)
                    peerConnectionMap[socketId]?.setLocalDescription(SdpAdapter(), p0)
                    SignalingClient.get()?.sendSessionDescription(p0!!, socketId)
                }
            }, MediaConstraints())
        }
    }

    override fun onAnswerReceived(data: JSONObject?) {
        val socketId = data?.optString("from")
        val peerConnection = getOrCreatePeerConnection(socketId)

        peerConnection.setRemoteDescription(
            SdpAdapter(),
            SessionDescription(SessionDescription.Type.ANSWER, data?.optString("sdp"))
        )
    }

    override fun onIceCandidateReceived(data: JSONObject?) {
        val socketId = data?.optString("from")

        val peerConnection = getOrCreatePeerConnection(socketId)

        peerConnection.addIceCandidate(
            IceCandidate(
                data?.optString("id"),
                data?.optInt("label") ?: -1,
                data?.optString("candidate")
            )
        )
    }

    private fun createCameraCapturer(isFront: Boolean): VideoCapturer? {
        val enumerator = Camera1Enumerator(false)

        val deviceNames = enumerator.deviceNames

        for (deviceName in deviceNames) {
            val deviceFront =
                if (isFront) enumerator.isFrontFacing(deviceName)
                else enumerator.isBackFacing(deviceName)

            if (deviceFront) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        return null
    }
}
