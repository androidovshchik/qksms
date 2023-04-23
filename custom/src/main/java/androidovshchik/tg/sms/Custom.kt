package androidovshchik.tg.sms

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony.Sms
import androidovshchik.tg.sms.local.Database
import androidovshchik.tg.sms.local.Message
import androidovshchik.tg.sms.local.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.flattener.PatternFlattener
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

internal lateinit var db: Database

object Custom {

    fun init(context: Context) {
        db = Room.databaseBuilder(context, Database::class.java, "custom.db")
            .fallbackToDestructiveMigration()
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
        AndroidThreeTen.init(context)
        val externalDir = context.getExternalFilesDir(null)
        val config = LogConfiguration.Builder()
            .enableThreadInfo()
            .build()
        val filePrinter = FilePrinter.Builder(externalDir?.path)
            .fileNameGenerator(DateFileNameGenerator())
            .backupStrategy(NeverBackupStrategy())
            .flattener(PatternFlattener("{d yyyy-MM-dd HH:mm:ss.SSS} {l}: {m}"))
            .build()
        XLog.init(config, filePrinter)
        val preferences = Preferences(context)
        Timber.plant(LogTree(preferences.saveLogs))
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            XLog.e(t.name, e)
        }
    }

    fun saveSms(values: ContentValues) {
        saveSms(Message(
            text = values.getAsString(Sms.BODY),
            address = values.getAsString(Sms.ADDRESS),
            datetime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(values.getAsLong(Sms.DATE_SENT)), ZoneOffset.UTC)
        ))
    }

    fun saveSms(message: Message) {
        db.messageDao().insert(message)
    }
}