package androidovshchik.tg.sms.ext

import okhttp3.OkHttpClient

fun OkHttpClient.cancelAll() {
    for (call in dispatcher.queuedCalls()) {
        call.cancel()
    }
    for (call in dispatcher.runningCalls()) {
        call.cancel()
    }
}