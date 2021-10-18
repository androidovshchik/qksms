package androidovshchik.tg.sms

import android.content.Context
import com.chibatching.kotpref.KotprefModel

private val numbers = '0'..'9'
private val uprLetters = 'A'..'Z'
private val lwrLetters = 'a'..'z'
private val letters = uprLetters + lwrLetters
private val chars = numbers + letters

class Preferences(context: Context) : KotprefModel(context) {

    override val kotprefName: String = "${context.packageName}_preferences"

    var botToken by nullableStringPref(null, "bot_token")

    var authCode by stringPref((0..8).map { chars.random() }.joinToString(""), "auth_code")

    var lastSmsId by intPref(-1, "last_sms_id")

    var lastUpdateId by intPref(-1, "last_update_id")

    val allowedChats by stringSetPref(linkedSetOf(), "allowed_chats")

    val allowedChatIds: List<Long>
        get() = allowedChats.map { it.toLong() }
}