package xyz.abc.flutter_nim.help

import com.netease.nimlib.sdk.NIMSDK
import com.netease.nimlib.sdk.RequestCallbackWrapper
import com.netease.nimlib.sdk.ResponseCode.RES_SUCCESS
import com.netease.nimlib.sdk.msg.model.RecentContact
import com.netease.nimlib.sdk.uinfo.model.NimUserInfo
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

                refashUserInfo(this)
            }
        } else {
            contact.clear()

            data?.apply {
                contact.addAll(this)
            }
        }

        pushMsg2Flutter()
    }

    private fun refashUserInfo(data: List<RecentContact>) {
        data.map { it.fromAccount }.toMutableList().apply {
            val items = NIMSDK.getUserService().getUserInfoList(this)

            if (items == null) {
                fetchUserInfo(this)
            } else {
                removeAll(items.map { it.account })

                fetchUserInfo(this)
            }
        }
    }

    private fun fetchUserInfo(accounts: List<String>) {
        NIMSDK.getUserService()
                .fetchUserInfo(accounts)
                .setCallback(object : RequestCallbackWrapper<List<NimUserInfo>>() {
                    override fun onResult(code: Int, result: List<NimUserInfo>?, exception: Throwable?) {
                        when (code.toShort()) {
                            RES_SUCCESS -> pushMsg2Flutter()
                        }
                    }
                })
    }

    private fun pushMsg2Flutter() {
        eventSink[0]?.success(DataParser.handleRecentSessionsData(contact))
    }
}