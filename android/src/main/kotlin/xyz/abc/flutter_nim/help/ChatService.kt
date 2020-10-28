package xyz.abc.flutter_nim.help

import android.util.Log
import com.netease.nimlib.sdk.NIMSDK
import com.netease.nimlib.sdk.RequestCallbackWrapper
import com.netease.nimlib.sdk.msg.MessageBuilder
import com.netease.nimlib.sdk.msg.MsgService.MSG_CHATTING_ACCOUNT_NONE
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum
import com.netease.nimlib.sdk.msg.model.IMMessage
import io.flutter.plugin.common.EventChannel.EventSink
import java.io.File

class ChatService constructor(private val sessionId: String, private val eventSink: Array<EventSink?>) {
    private val msgService = NIMSDK.getMsgService()
    private val chatMsg = mutableMapOf<String, IMMessage>()

    init {
        msgService
                .pullMessageHistory(MessageBuilder.createEmptyMessage(sessionId, SessionTypeEnum.P2P, 0), 40, true)
                .setCallback(object : RequestCallbackWrapper<List<IMMessage>?>() {
                    override fun onResult(code: Int, result: List<IMMessage>?, exception: Throwable?) {
                        result?.reversed()?.apply {
                            forEach {
                                chatMsg[it.uuid] = it
                            }

                            pushMsg2Flutter()
                        }
                    }
                })

        msgService.setChattingAccount(sessionId, SessionTypeEnum.P2P)
    }

    fun onDestroy() {
        msgService.setChattingAccount(MSG_CHATTING_ACCOUNT_NONE, SessionTypeEnum.None)
    }

    fun observeMsgStatus(msg: IMMessage) {
        if (chatMsg.containsKey(msg.uuid)) {
            chatMsg[msg.uuid] = msg

            pushMsg2Flutter()
        }
    }

    fun onMessageIncoming(list: List<IMMessage>) {
        var b = false

        list.filter { it.sessionId == sessionId }.forEach {
            chatMsg[it.uuid] = it

            b = true
        }

        if (b) pushMsg2Flutter()
    }

    fun sendTextMessage(text: String) {
        val msg = MessageBuilder.createTextMessage(
                sessionId,
                SessionTypeEnum.P2P,
                text
        )

        sendMsg(msg)
    }

    fun sendImageMessage(path: String) {
        val msg = MessageBuilder.createImageMessage(
                sessionId,
                SessionTypeEnum.P2P,
                File(path)
        )

        sendMsg(msg)
    }

    fun sendAudioMessage(audio: Pair<File, Long>) {
        val msg = MessageBuilder.createAudioMessage(
                sessionId,
                SessionTypeEnum.P2P,
                audio.first,
                audio.second
        )

        sendMsg(msg)
    }

    private fun sendMsg(msg: IMMessage) {
        Log.d("ChatService", "发送消息 ${msg.msgType} # ${msg.uuid}")

        msgService.sendMessage(msg, false)

        chatMsg[msg.uuid] = msg

        pushMsg2Flutter()
    }

    private fun pushMsg2Flutter() {
        eventSink[0]?.success(DataParser.handleMessages(chatMsg.values.toList()))
    }
}