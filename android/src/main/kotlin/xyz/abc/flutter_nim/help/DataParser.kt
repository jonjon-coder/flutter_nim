package xyz.abc.flutter_nim.help

import com.netease.nimlib.sdk.NIMSDK
import com.netease.nimlib.sdk.msg.attachment.AudioAttachment
import com.netease.nimlib.sdk.msg.attachment.ImageAttachment
import com.netease.nimlib.sdk.msg.attachment.VideoAttachment
import com.netease.nimlib.sdk.msg.constant.AttachStatusEnum
import com.netease.nimlib.sdk.msg.constant.MsgDirectionEnum
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum
import com.netease.nimlib.sdk.msg.model.IMMessage
import com.netease.nimlib.sdk.msg.model.RecentContact
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object DataParser {
    // 处理最近会话数据
    fun handleRecentSessionsData(recents: List<RecentContact>?): String {
        var result = ""
        if (recents != null) {
            val recentSessionJSONArray = JSONArray()
            for (i in recents.indices) {
                val recent = recents[i]

                // 最近联系人ID
                val contactId = recent.contactId
                val recentObject = JSONObject()
                try {
                    recentObject.put("sessionId", recent.contactId)
                    recentObject.put("unreadCount", recent.unreadCount)
                    recentObject.put("timestamp", recent.time)
                    recentObject.put("messageContent", recent.content)

                    // 最后一条消息信息
                    val lastMessageObject = JSONObject()

                    lastMessageObject.put("messageId", recent.recentMessageId)
                    lastMessageObject.put("from", recent.fromAccount)
                    lastMessageObject.put("text", recent.content)
                    lastMessageObject.put("messageType", recent.msgType.value)
                    lastMessageObject.put("timestamp", recent.time)

                    if (recent.msgType == MsgTypeEnum.custom) {
                        val customAttachment = recent.attachment as? JsonAttachment

                        lastMessageObject.put(
                                "customMessageContent",
                                customAttachment?.toJson(false) ?: ""
                        )
                    }

                    when (recent.msgStatus) {
                        MsgStatusEnum.fail -> lastMessageObject.put("deliveryState", 0)
                        MsgStatusEnum.sending -> lastMessageObject.put("deliveryState", 1)
                        MsgStatusEnum.success -> lastMessageObject.put("deliveryState", 2)
                        else -> {
                        }
                    }
                    recentObject.put("lastMessage", lastMessageObject)

                    // 用户信息
                    val userObject = JSONObject()
                    val userInfo = NIMSDK.getUserService().getUserInfo(contactId)
                    if (userInfo != null) {
                        userObject.put("nickname", userInfo.name)
                        userObject.put("avatarUrl", userInfo.avatar)
                        userObject.put("userExt", userInfo.extension)
                    }
                    recentObject.put("userInfo", userObject)
                    recentSessionJSONArray.put(recentObject)
                } catch (exception: JSONException) {
                    exception.printStackTrace()
                }
            }
            val imObject = JSONObject()
            try {
                imObject.put("recentSessions", recentSessionJSONArray)
            } catch (exception: JSONException) {
                exception.printStackTrace()
            }
            result = imObject.toString()
            return result
        }
        return result
    }

    // 处理会话消息数据
    fun handleMessages(messages: List<IMMessage>?): String {
        var result = ""
        if (messages != null) {
            val messageJSONArray = JSONArray()
            for (i in messages.indices) {
                val message = messages[i]
                messageJSONArray.put(getMessageJSONObject(message))
            }
            val imObject = JSONObject()
            try {
                imObject.put("messages", messageJSONArray)
            } catch (exception: JSONException) {
                exception.printStackTrace()
            }
            result = imObject.toString()
            return result
        }
        return result
    }

    // 可用的消息对象
    private fun getMessageJSONObject(message: IMMessage): JSONObject {
        val json = JSONObject()
        try {
            json.put("messageId", message.uuid)
            json.put("from", message.fromAccount)
            json.put("text", message.content)
            json.put("messageType", message.msgType.value)
            json.put("timestamp", message.time)

            when (message.direct) {
                MsgDirectionEnum.In -> json.put("isOutgoingMsg", false)
                MsgDirectionEnum.Out -> json.put("isOutgoingMsg", true)
            }
            when (message.status) {
                MsgStatusEnum.fail -> json.put("deliveryState", 0)
                MsgStatusEnum.sending -> json.put("deliveryState", 1)
                MsgStatusEnum.success -> json.put("deliveryState", 2)
                else -> {
                }
            }
            when (message.msgType) {
                MsgTypeEnum.image -> {
                    val imageAttachment = message.attachment as ImageAttachment
                    val img = JSONObject()

                    img.put("url", imageAttachment.url)
                    img.put("thumbUrl", imageAttachment.thumbUrl)
                    img.put("thumbPath", imageAttachment.thumbPath)
                    img.put("path", imageAttachment.path)
                    img.put("width", imageAttachment.width)
                    img.put("height", imageAttachment.height)

                    json.put("messageObject", img)
                }
                MsgTypeEnum.audio -> {
                    val audioAttachment = message.attachment as AudioAttachment
                    val audio = JSONObject()

                    audio.put("url", audioAttachment.url)
                    audio.put("path", audioAttachment.path)
                    audio.put("duration", audioAttachment.duration)
                    audio.put("isPlayed", !isUnreadAudioMessage(message))

                    json.put("messageObject", audio)
                }
                MsgTypeEnum.video -> {
                    val videoAttachment = message.attachment as VideoAttachment
                    val video = JSONObject()

                    video.put("url", videoAttachment.url)
                    video.put("coverUrl", videoAttachment.thumbUrl)
                    video.put("path", videoAttachment.path)
                    video.put("duration", videoAttachment.duration)
                    video.put("width", videoAttachment.width)
                    video.put("height", videoAttachment.height)

                    json.put("messageObject", video)
                }
                MsgTypeEnum.custom -> {
                    val customAttachment = message.attachment as? JsonAttachment

                    json.put(
                            "customMessageContent",
                            customAttachment?.toJson(false) ?: ""
                    )
                }
                else -> {
                }
            }
        } catch (exception: JSONException) {
            exception.printStackTrace()
        }

        return json
    }

    private fun isUnreadAudioMessage(message: IMMessage): Boolean {
        return message.msgType == MsgTypeEnum.audio &&
                message.direct == MsgDirectionEnum.In &&
                message.attachStatus == AttachStatusEnum.transferred &&
                message.status != MsgStatusEnum.read
    }
}