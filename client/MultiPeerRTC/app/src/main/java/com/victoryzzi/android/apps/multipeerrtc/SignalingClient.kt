package com.victoryzzi.android.apps.multipeerrtc

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.net.URISyntaxException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.security.cert.CertificateException
import javax.security.cert.X509Certificate


class SignalingClient {
    private lateinit var callback: Callback
    private lateinit var socket: Socket

    private val room = "OldPlace"


//    private val trustAll =
//        arrayOf<TrustManager>(
//            object : X509TrustManager {
//                @Throws(CertificateException::class)
//                fun checkClientTrusted(
//                    chain: Array<X509Certificate?>?,
//                    authType: String?
//                ) {
//                }
//
//                @Throws(CertificateException::class)
//                fun checkServerTrusted(
//                    chain: Array<X509Certificate?>?,
//                    authType: String?
//                ) {
//                }
//
//                val acceptedIssuers: Array<X509Certificate?>
//                    get() = arrayOfNulls(0)
//
//                override fun checkClientTrusted(
//                    p0: Array<out java.security.cert.X509Certificate>?,
//                    p1: String?
//                ) {
//                }
//
//                override fun checkServerTrusted(
//                    p0: Array<out java.security.cert.X509Certificate>?,
//                    p1: String?
//                ) {
//                }
//
//                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
//                    return emptyArray()
//                }
//            }
//        )

    fun init(callback: Callback) {
        this.callback = callback
        try {
//            val sslContext = SSLContext.getInstance("TLS").apply { init(null, trustAll, null) }
//            IO.setDefaultSSLContext(sslContext)

            IO.setDefaultHostnameVerifier { _, _ ->
                true
            }
            socket = IO.socket("http://13.125.192.145:8080")
            socket.connect()

            socket.emit("create or join", room)

            socket.on("created") { _ ->
                loge("room created: ${socket.id()}")
                callback.onCreateRoom()
            }

            socket.on("full") { _ ->
                loge("room full")
            }

            socket.on("join") { args: Array<out Any>? ->
                loge("peer joined ${Arrays.toString(args)}")
                callback.onPeerJoined(args?.get(1).toString())
            }

            socket.on("joined") { _ ->
                loge("self joined : ${socket.id()}")
                callback.onSelfJoined()
            }

            socket.on("log") { args ->
                loge("log call : ${args[0]}")
            }

            socket.on("bye") { args ->
                loge("bye ${args[0]}")
                callback.onPeerLeave(args[0].toString())
            }

            socket.on("message") { args ->
                loge("message ${Arrays.toString(args)}")

                val arg = args[0]

                if (arg is String) {

                } else if (arg is JSONObject) {
                    val data = arg as JSONObject
                    val type = data.optString("type")

                    when (type) {
                        "offer" -> {
                            callback.onOfferReceived(data)
                        }

                        "answer" -> {
                            callback.onAnswerReceived(data)
                        }

                        "candidate" -> {
                            callback.onIceCandidateReceived(data)
                        }
                    }
                }
            }
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        socket.emit("bye", socket.id())
        socket.disconnect()
        socket.close()
        instance = null
    }

    fun sendIceCandidate(iceCandidate: IceCandidate, to: String?) {
        val jo = JSONObject()
        try {
            jo.put("type", "candidate")
            jo.put("label", iceCandidate.sdpMLineIndex)
            jo.put("id", iceCandidate.sdpMid)
            jo.put("candidate", iceCandidate.sdp)
            jo.put("from", socket.id())
            jo.put("to", to)
            socket.emit("message", jo)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun sendSessionDescription(sdp: SessionDescription, to: String?) {
        val jo = JSONObject()
        try {
            jo.put("type", sdp.type.canonicalForm())
            jo.put("sdp", sdp.description)
            jo.put("from", socket.id())
            jo.put("to", to)
            socket.emit("message", jo)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    interface Callback {
        fun onCreateRoom()
        fun onPeerJoined(socketId: String?)
        fun onSelfJoined()
        fun onPeerLeave(msg: String?)
        fun onOfferReceived(data: JSONObject?)
        fun onAnswerReceived(data: JSONObject?)
        fun onIceCandidateReceived(data: JSONObject?)
    }

    companion object {
        var instance: SignalingClient? = null
        fun get(): SignalingClient? {
            if (instance == null) {
                synchronized(SignalingClient::class.java) {
                    if (instance == null) {
                        instance = SignalingClient()
                    }
                }
            }
            return instance
        }
    }
}