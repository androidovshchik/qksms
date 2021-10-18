package androidovshchik.tg.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class ToastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.hasExtra(EXTRA_MESSAGE)) {
            Toast.makeText(
                context,
                intent.getStringExtra(EXTRA_MESSAGE),
                intent.getIntExtra(EXTRA_DURATION, Toast.LENGTH_SHORT)
            ).show()
        }
    }

    companion object {

        const val EXTRA_MESSAGE = "extra_message"

        const val EXTRA_DURATION = "extra_duration"
    }
}