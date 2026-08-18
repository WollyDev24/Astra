package dev.wolly.dsbmaterial

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.wolly.dsbmaterial.api.DSBAuthException
import dev.wolly.dsbmaterial.api.DSBMobileAPI
import dev.wolly.dsbmaterial.data.DataStoreManager
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import kotlinx.coroutines.flow.first

class AutoFetchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dataStoreManager = DataStoreManager(applicationContext)
        val username = dataStoreManager.usernameFlow.first() ?: return Result.failure()
        val password = dataStoreManager.passwordFlow.first() ?: return Result.failure()
        val className = dataStoreManager.classNameFlow.first() ?: return Result.failure()
        if (username.isEmpty() || password.isEmpty()) return Result.failure()

        val gson = Gson()
        val type = object : TypeToken<List<SubstitutionEntry>>() {}.type
        val lastKnownJson = dataStoreManager.archiveFlow.first() ?: ""
        val lastKnown = if (lastKnownJson.isNotEmpty()) {
            try {
                gson.fromJson<List<SubstitutionEntry>>(lastKnownJson, type).orEmpty()
            } catch (_: Exception) { emptyList() }
        } else emptyList()

        val customServerUrl = dataStoreManager.customServerUrlFlow.first() ?: ""

        return try {
            val api = DSBMobileAPI(username, password, customServerUrl.trimEnd('/'))
            val allRaw = api.getSubstitutions("")

            val allClassNames = mutableSetOf(className)
            val selectedClassesStr = dataStoreManager.selectedClassesFlow.first()
            if (!selectedClassesStr.isNullOrEmpty()) {
                allClassNames.addAll(selectedClassesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            }

            val filtered = allRaw.filter { entry ->
                allClassNames.any { cls -> entry.className.equals(cls, ignoreCase = true) }
            }

            val deduped = filtered.distinctBy { it.day + it.lesson + it.subject + it.room + it.art + it.text }

            val newArchive = (deduped + lastKnown).distinctBy {
                it.day + it.lesson + it.subject + it.room + it.art + it.text
            }

            if (newArchive.size != lastKnown.size) {
                val lastKnownKeys = HashSet<String>(lastKnown.size * 2)
                lastKnown.forEach { lastKnownKeys.add(dedupKey(it)) }
                val newEntries = deduped.filter { lastKnownKeys.add(dedupKey(it)) }

                dataStoreManager.saveArchive(gson.toJson(newArchive))
                if (newEntries.isNotEmpty()) {
                    sendNotification(newEntries)
                }
            }

            Result.success()
        } catch (e: DSBAuthException) {
            Result.failure()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(newEntries: List<SubstitutionEntry>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val context = applicationContext
        val firstLine = notificationLine(context, newEntries.first())
        val summary = if (newEntries.size > 1) {
            context.getString(R.string.notif_summary_multi, firstLine, newEntries.size - 1)
        } else {
            firstLine
        }

        val builder = NotificationCompat.Builder(context, DSBApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_calendar)
            .setContentTitle(context.getString(R.string.notif_title, newEntries.size))
            .setContentText(summary)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (newEntries.size > 1) {
            val bigText = buildString {
                newEntries.forEachIndexed { index, entry ->
                    if (index > 0) append('\n')
                    append(notificationLine(context, entry))
                }
            }
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    private fun dedupKey(entry: SubstitutionEntry): String =
        entry.day + "|" + entry.lesson + "|" + entry.subject + "|" + entry.room + "|" + entry.art + "|" + entry.text

    private fun notificationLine(context: Context, entry: SubstitutionEntry): String {
        val day = entry.day.substringBefore(',').trim()
        val lesson = if (entry.lesson.isNotEmpty()) {
            context.getString(R.string.format_notif_lesson, entry.lesson)
        } else null
        val subjectPart = when {
            entry.art.isNotEmpty() && entry.subject.isNotEmpty() -> "${entry.subject} (${entry.art})"
            entry.subject.isNotEmpty() -> entry.subject
            entry.art.isNotEmpty() -> entry.art
            else -> "—"
        }
        val room = if (entry.room.isNotEmpty()) {
            context.getString(R.string.format_notif_room, entry.room)
        } else null
        val parts = listOfNotNull(lesson, subjectPart, room)
        return buildString {
            if (day.isNotEmpty()) {
                append(day)
                append(": ")
            }
            append(parts.joinToString(" – "))
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "dsb_auto_fetch"
    }
}
