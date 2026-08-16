package dev.wolly.dsbmaterial.api

import android.util.Log
import dev.wolly.dsbmaterial.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class AppUpdate(
    val version: String,
    val name: String,
    val publishedAt: String,
    val downloadUrl: String
)

data class GitCommit(
    val sha: String,
    val message: String,
    val author: String,
    val date: String
)

enum class UpdateChannel(val key: String) {
    STABLE("stable"),
    BETA("beta"),
    DEV("dev");

    companion object {
        fun fromKey(key: String): UpdateChannel =
            entries.firstOrNull { it.key == key } ?: STABLE
    }
}

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val GITHUB_BASE = "https://api.github.com/repos/wollydev24/astra"
    private const val RELEASES_LATEST_URL = "$GITHUB_BASE/releases/latest"
    private const val RELEASES_URL = "$GITHUB_BASE/releases"
    private const val DEV_RELEASE_TAG = "dev"
    private const val DEV_RELEASE_URL = "$GITHUB_BASE/releases/tags/dev"
    private const val COMMITS_URL = "$GITHUB_BASE/commits"

    fun isUpdateAvailable(latest: String): Boolean {
        val current = parseVersion(BuildConfig.VERSION_NAME)
        val candidate = parseVersion(latest)
        val length = maxOf(current.size, candidate.size)
        for (i in 0 until length) {
            val c = current.getOrNull(i) ?: 0
            val l = candidate.getOrNull(i) ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun parseVersion(version: String): List<Int> =
        version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split(".", "-", "+")
            .mapNotNull { it.toIntOrNull() }

    private fun newRequest(url: String): Request = Request.Builder()
        .url(url)
        .header("Accept", "application/vnd.github+json")
        .header("User-Agent", "Astra")
        .build()

    private fun parseRelease(json: JSONObject): AppUpdate {
        val version = json.optString("tag_name", "").removePrefix("v")
        val assets = json.optJSONArray("assets") ?: JSONArray()
        var downloadUrl = ""
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name", "").endsWith(".apk", ignoreCase = true)) {
                downloadUrl = asset.optString("browser_download_url", "")
                break
            }
        }
        if (downloadUrl.isEmpty()) downloadUrl = json.optString("html_url", "")
        return AppUpdate(
            version = version,
            name = json.optString("name", version),
            publishedAt = json.optString("published_at", ""),
            downloadUrl = downloadUrl
        )
    }

    suspend fun checkLatest(channel: UpdateChannel): AppUpdate? = withContext(Dispatchers.IO) {
        try {
            val url = when (channel) {
                UpdateChannel.STABLE -> RELEASES_LATEST_URL
                UpdateChannel.BETA -> "$RELEASES_URL?per_page=5"
                UpdateChannel.DEV -> DEV_RELEASE_URL
            }
            val response = DSBNetwork.client.newCall(newRequest(url)).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "GitHub API responded ${response.code}")
                return@withContext null
            }
            val body = response.body?.string() ?: return@withContext null
            if (channel == UpdateChannel.BETA) {
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val release = array.optJSONObject(i) ?: continue
                    if (release.optString("tag_name", "") == DEV_RELEASE_TAG) continue
                    if (release.optBoolean("draft", false)) continue
                    return@withContext parseRelease(release)
                }
                return@withContext null
            }
            parseRelease(JSONObject(body))
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            null
        }
    }

    suspend fun fetchCommits(limit: Int = 15): List<GitCommit>? = withContext(Dispatchers.IO) {
        try {
            val response = DSBNetwork.client.newCall(newRequest("$COMMITS_URL?path=app/&per_page=$limit")).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "GitHub commits API responded ${response.code}")
                return@withContext null
            }
            val body = response.body?.string() ?: return@withContext null
            val array = JSONArray(body)
            val commits = mutableListOf<GitCommit>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val commit = item.optJSONObject("commit") ?: continue
                val author = commit.optJSONObject("author")
                val rawMessage = commit.optString("message", "").trim()
                val message = rawMessage.lineSequence().firstOrNull()?.trim().orEmpty()
                commits += GitCommit(
                    sha = item.optString("sha", "").take(7),
                    message = message,
                    author = author?.optString("name", "") ?: "",
                    date = author?.optString("date", "") ?: ""
                )
            }
            commits
        } catch (e: Exception) {
            Log.e(TAG, "Commit fetch failed", e)
            null
        }
    }
}
