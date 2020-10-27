package xyz.abc.flutter_nim.help

import com.netease.nimlib.sdk.NIMSDK
import com.netease.nimlib.sdk.RequestCallbackWrapper
import com.netease.nimlib.sdk.msg.MessageBuilder
import com.netease.nimlib.sdk.msg.MsgService.MSG_CHATTING_ACCOUNT_NONE
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum
import com.netease.nimlib.sdk.msg.model.IMMessage
import io.flutter.plugin.common.EventChannel
import java.io.File

class ChatService constructor(private val sessionId: String, private val eventSink: EventChannel.EventSink) {
    private val msgService = NIMSDK.getMsgService()
    private val data = mutableMapOf<String, IMMessage>()

    init {
        msgService
                .pullMessageHistory(MessageBuilder.createEmptyMessage(sessionId, SessionTypeEnum.P2P, 0), 40, true)
                .setCallback(object : RequestCallbackWrapper<List<IMMessage>?>() {
                    override fun onResult(code: Int, result: List<IMMessage>?, exception: Throwable?) {
                        result?.apply {
                            forEach {
                                data[it.uuid] = it
                            }

                            eventSink.success(NIMSessionParser.handleMessages(this))
                        }
                    }
                })

        msgService.setChattingAccount(sessionId, SessionTypeEnum.P2P)
    }

    fun onDestroy() {
        msgService.setChattingAccount(MSG_CHATTING_ACCOUNT_NONE, SessionTypeEnum.None)
    }

    fun observeMsgStatus(msg: IMMessage) {
        if (data.containsKey(msg.uuid)) {
            data[msg.uuid] = msg

            eventSink.success(NIMSessionParser.handleMessages(data.values.toList()))
        }
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

    fun sendVideoMessage(path: String) {

    }

    fun sendAudioMessage(path: String) {
        val msg = MessageBuilder.createAudioMessage(
                sessionId,
                SessionTypeEnum.P2P,
                File(path),
                0
        )

        sendMsg(msg)
    }

    private fun sendMsg(msg: IMMessage) {
        msgService.sendMessage(msg, false)

        data[msg.uuid] = msg

        eventSink.success(NIMSessionParser.handleMessages(data.values.toList()))
    }
}