package xyz.abc.flutter_nim

import android.util.Log
import androidx.annotation.NonNull
import com.netease.nimlib.sdk.InvocationFuture
import com.netease.nimlib.sdk.NIMSDK
import com.netease.nimlib.sdk.RequestCallback
import com.netease.nimlib.sdk.ResponseCode
import com.netease.nimlib.sdk.auth.LoginInfo
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import xyz.abc.flutter_nim.help.AudioService
import xyz.abc.flutter_nim.help.ChatService
import xyz.abc.flutter_nim.help.JsonAttachment
import xyz.abc.flutter_nim.help.SessionService

/** FlutterNimPlugin */
class FlutterNimPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
    companion object {
        private const val METHOD_CHANNEL_NAME = "flutter_nim_method"
        private const val EVENT_CHANNEL_NAME = "flutter_nim_event"

        private const val METHOD_IM_INIT = "imInit"
        private const val METHOD_IM_LOGIN = "imLogin"
        private const val METHOD_IM_LOGOUT = "imLogout"
        private const val METHOD_IM_RECENT_SESSIONS = "imRecentSessions"
        private const val METHOD_IM_DELETE_RECENT_SESSION = "imDeleteRecentSession"
        private const val METHOD_IM_START_CHAT = "imStartChat"
        private const val METHOD_IM_EXIT_CHAT = "imExitChat"
        private const val METHOD_IM_MESSAGES = "imMessages"
        private const val METHOD_IM_SEND_TEXT = "imSendText"
        private const val METHOD_IM_SEND_IMAGE = "imSendImage"
        private const val METHOD_IM_SEND_VIDEO = "imSendVideo"
        private const val METHOD_IM_SEND_AUDIO = "imSendAudio"
        private const val METHOD_IM_SEND_CUSTOM = "imSendCustom"
        private const val METHOD_IM_SEND_CUSTOM_2 = "imSendCustomToSession"
        private const val METHOD_IM_RESEND_MESSAGE = "imResendMessage"
        private const val METHOD_IM_MARK_READ = "imMarkAudioMessageRead"
        private const val METHOD_IM_RECORD_START = "onStartRecording"
        private const val METHOD_IM_RECORD_STOP = "onStopRecording"
        private const val METHOD_IM_RECORD_CANCEL = "onCancelRecording"
    }

    private val eventSink = arrayOfNulls<EventSink?>(1)
    private val chatService = arrayOfNulls<ChatService?>(1)
    private val sessionService = SessionService(eventSink)

    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var audioService: AudioService

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, METHOD_CHANNEL_NAME)
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, EVENT_CHANNEL_NAME)

        methodChannel.setMethodCallHandler(this)
        eventChannel.setStreamHandler(this)

        audioService = AudioService(flutterPluginBinding.applicationContext)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            METHOD_IM_INIT -> {
                NIMSDK.getMsgServiceObserve().apply {
                    // 消息状态改变
                    observeMsgStatus({
                        Log.d("observeMsgStatus", "${it.uuid}#${it.status}")

                        chatService[0]?.observeMsgStatus(it)
                    }, true)

                    // 新消息
                    observeReceiveMessage({
                        Log.d("observeReceiveMessage", "${it.size}")

                        chatService[0]?.onMessageIncoming(it)
                    }, true)

                    // 最新联系人
                    observeRecentContact({
                        Log.d("observeRecentContact", "${it.size}")

                        sessionService.onRecentContact(it)
                    }, true)
                }

                NIMSDK.getMsgService().apply {
                    registerCustomAttachmentParser { JsonAttachment(it) }
                }

                result.success(true)
            }
            METHOD_IM_LOGIN -> {
                val info = LoginInfo(call.argument("imAccount"), call.argument("imToken"))

                NIMSDK.getAuthService()
                        .login(info).then(result) { true }
            }
            METHOD_IM_LOGOUT -> {
                NIMSDK.getAuthService()
                        .logout()

                result.success(true)
            }
            METHOD_IM_RECENT_SESSIONS -> {
                sessionService.queryRecentContacts()

                result.success(true)
            }
            METHOD_IM_START_CHAT -> {
                chatService[0] = ChatService(call.argument("sessionId")!!, eventSink)

                result.success(true)
            }
            METHOD_IM_EXIT_CHAT -> {
                chatService[0]!!.onDestroy()
                chatService[0] = null

                result.success(true)
            }
            METHOD_IM_MESSAGES -> {
                val messageIndex: Int? = call.argument("messageIndex")

                result.success(true)
            }
            METHOD_IM_SEND_TEXT -> {
                chatService[0]!!.sendTextMessage(call.argument("text")!!)

                result.success(true)
            }
            METHOD_IM_SEND_IMAGE -> {
                chatService[0]!!.sendImageMessage(call.argument("imagePath")!!)

                result.success(true)
            }
            METHOD_IM_RECORD_START -> {
                audioService.start()

                result.success(true)
            }
            METHOD_IM_RECORD_CANCEL -> {
                audioService.cancel()

                result.success(true)
            }
            METHOD_IM_RECORD_STOP -> {
                val audio = audioService.stop()

                audio?.apply {
                    if (audio.second > 1000) {
                        chatService[0]!!.sendAudioMessage(audio)
                    } else {
                        Log.d("", "录音时间小于1秒，忽略发送")
                    }
                }

                result.success(true)
            }
        }
    }

    override fun onListen(arguments: Any?, events: EventSink?) {
        eventSink[0] = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink[0] = null
    }

    private fun <T, R> InvocationFuture<T>.then(result: Result, block: (T) -> R) = setCallback(Callback<T, R>(result, block))
    private fun <T> InvocationFuture<T>.then(result: Result) = setCallback(Callback<T, T>(result) { it })
}

private class Callback<T, R> constructor(private val result: Result, private val trans: (T) -> R) : RequestCallback<T> {

    override fun onSuccess(param: T) = result.success(trans(param))

    override fun onFailed(code: Int) = result.error("$code", null, null)

    override fun onException(exception: Throwable?) = result.error("${ResponseCode.RES_EXCEPTION}", exception?.message, null)
}