package xyz.abc.flutter_nim.help

import com.netease.nimlib.sdk.msg.attachment.MsgAttachment

internal class JsonAttachment(private val json: String) : MsgAttachment {
    override fun toJson(send: Boolean) = json
}