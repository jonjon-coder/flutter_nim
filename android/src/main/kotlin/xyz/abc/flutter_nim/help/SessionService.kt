package xyz.abc.flutter_nim.help

import com.netease.nimlib.sdk.NIMSDK
import com.netease.nimlib.sdk.RequestCallbackWrapper
import com.netease.nimlib.sdk.msg.model.RecentContact
import io.flutter.plugin.common.EventChannel

class SessionService constructor(private val eventSink: Array<EventChannel.EventSink?>) {
    private val msgService = NIMSDK.getMsgService()
    private val contact = mutableListOf<RecentContact>()

    fun queryRecentContacts() {
        msgService
                .queryRecentContacts()
                .setCallback(object : RequestCallbackWrapper<List<RecentContact>>() {
                    override fun onResult(code: Int, result: List<RecentContact>?, exception: Throwable?) {
                        onRecentContact(result, false)
                    }
                })
    }

    fun onRecentContact(data: List<RecentContact>?, append: Boolean = true) {
        if (append) {
            data?.apply {
                val ids = map { it.contactId }

                contact.removeAll {
                    ids.contains(it.contactId)
                }

                contact.addAll(0, this)
            }
        } else {
            contact.clear()

            data?.apply {
                contact.addAll(this)
            }
        }

        eventSink[0]?.success(DataParser.handleRecentSessionsData(contact))
    }
}