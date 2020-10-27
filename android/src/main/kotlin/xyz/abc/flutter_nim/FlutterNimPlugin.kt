package xyz.abc.flutter_nim

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
import xyz.abc.flutter_nim.help.ChatService
import xyz.abc.flutter_nim.help.NIMSessionParser

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

    private var eventSink: EventSink? = null
    private var chatService: ChatService? = null

    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, METHOD_CHANNEL_NAME)
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, EVENT_CHANNEL_NAME)

        methodChannel.setMethodCallHandler(this)
        eventChannel.setStreamHandler(this)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            METHOD_IM_INIT -> {
                NIMSDK.getMsgServiceObserve()
                        .observeMsgStatus({ chatService?.observeMsgStatus(it) }, true)

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
                NIMSDK.getMsgService()
                        .queryRecentContacts()
                        .then(result) { NIMSessionParser.handleRecentSessionsData(it) }
            }
//            METHOD_IM_DELETE_RECENT_SESSION -> {
//                val deletedSessionId: String? = call.argument("sessionId")
//                recentSessionsInteractor.deleteRecentContact2(deletedSessionId)
//            }
            METHOD_IM_START_CHAT -> {
                chatService = ChatService(call.argument("sessionId")!!, eventSink!!)

                result.success(true)
            }
            METHOD_IM_EXIT_CHAT -> {
                chatService!!.onDestroy()
                chatService = null

                result.success(true)
            }
//            METHOD_IM_MESSAGES -> {
//                val messageIndex: Int? = call.argument("messageIndex")
//                sessionInteractor.loadHistoryMessages(messageIndex)
//            }
            METHOD_IM_SEND_TEXT -> {
                chatService!!.sendTextMessage(call.argument("text")!!)

                result.success(true)
            }
            METHOD_IM_SEND_IMAGE -> {
                chatService!!.sendImageMessage(call.argument("imagePath")!!)

                result.success(true)
            }
            METHOD_IM_SEND_VIDEO -> {
                chatService!!.sendVideoMessage(call.argument("videoPath")!!)

                result.success(true)
            }
            METHOD_IM_SEND_AUDIO -> {
                chatService!!.sendAudioMessage(call.argument("audioPath")!!)

                result.success(true)
            }
//            METHOD_IM_SEND_CUSTOM -> {
//                val customEncodeString: String? = call.argument("customEncodeString")
//                val apnsContent: String? = call.argument("apnsContent")
//                if (sessionInteractor != null) {
//                    sessionInteractor.sendCustomMessage(customEncodeString, apnsContent)
//                }
//            }
//            METHOD_IM_SEND_CUSTOM_2 -> {
//                val sessionId2: String? = call.argument("sessionId")
//                val customEncodeString2: String? = call.argument("customEncodeString")
//                val apnsContent2: String? = call.argument("apnsContent")
//                NIMSessionInteractor.sendCustomMessageToSession(sessionId2, customEncodeString2, apnsContent2)
//                result.success(true)
//            }
//            METHOD_IM_RESEND_MESSAGE -> {
//                val messageId: String? = call.argument("messageId")
//                if (sessionInteractor != null) {
//                    sessionInteractor.resendMessage(messageId)
//                }
//            }
//            METHOD_IM_MARK_READ -> {
//                val audioMessageId: String? = call.argument("messageId")
//                if (sessionInteractor != null) {
//                    sessionInteractor.markAudioMessageRead(audioMessageId)
//                    result.success(true)
//                }
//            }
//            METHOD_IM_RECORD_START -> if (sessionInteractor != null) {
//                sessionInteractor.onStartRecording()
//            }
//            METHOD_IM_RECORD_STOP -> if (sessionInteractor != null) {
//                sessionInteractor.onStopRecording()
//            }
//            METHOD_IM_RECORD_CANCEL -> if (sessionInteractor != null) {
//                sessionInteractor.onCancelRecording()
//            }
        }
    }

    override fun onListen(arguments: Any?, events: EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    private fun <T, R> InvocationFuture<T>.then(result: Result, block: (T) -> R) = setCallback(Callback<T, R>(result, block))
    private fun <T> InvocationFuture<T>.then(result: Result) = setCallback(Callback<T, T>(result) { it })
}

private class Callback<T, R> constructor(private val result: Result, private val trans: (T) -> R) : RequestCallback<T> {

    override fun onSuccess(param: T) = result.success(trans(param))

    override fun onFailed(code: Int) = result.error("$code", null, null)

    override fun onException(exception: Throwable?) = result.error("${ResponseCode.RES_EXCEPTION}", exception?.message, null)
}