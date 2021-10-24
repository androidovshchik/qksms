package androidovshchik.tg.sms

import com.elvishew.xlog.XLog
import timber.log.Timber

class LogTree(saveLogs: Boolean) : Timber.DebugTree() {

    init {
        saveToFile = saveLogs
    }

    override fun createStackElementTag(element: StackTraceElement): String {
        return "${super.createStackElementTag(element)}:${element.methodName}:${element.lineNumber}"
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        if (saveToFile) {
            XLog.log(priority, "$tag: $message", t)
        }
    }

    companion object {

        @Volatile
        internal var saveToFile = false
    }
}