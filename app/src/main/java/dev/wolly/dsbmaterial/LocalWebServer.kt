package dev.wolly.dsbmaterial

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.ui.theme.SeedPalettes
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale

object LocalWebServer {

    const val PORT = 8080
    private const val REFRESH_HEADER = "Access-Control-Allow-Origin"
    private const val MIME_HTML = "text/html; charset=utf-8"
    private const val MIME_JSON = "application/json; charset=utf-8"

    private val gson = Gson()
    private val entriesType = object : TypeToken<List<SubstitutionEntry>>() {}.type

    @Volatile
    private var server: NanoHTTPD? = null

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var cachedPage: String? = null

    @Volatile
    private var parsedEntries: List<SubstitutionEntry> = emptyList()

    @Volatile
    private var isRoomFirst: Boolean = true

    @Volatile
    private var sortByPeriod: Boolean = true

    @Volatile
    private var themeIndex: Int = 0

    @Volatile
    private var dynamicColor: Boolean = true

    @Volatile
    private var updatedAt: Long = 0L

    @Volatile
    private var cachedPayload: String? = null

    @Volatile
    private var cachedPayloadKey: String? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _urls = MutableStateFlow<List<String>>(emptyList())
    val urls: StateFlow<List<String>> = _urls

    /** Starts the HTTP server on all network interfaces. Returns true if it started. */
    fun start(context: Context): Boolean {
        if (server != null) return true
        appContext = context.applicationContext
        return try {
            val s = DSBWebServer(PORT)
            s.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            server = s
            _isRunning.value = true
            _urls.value = computeLocalUrls()
            true
        } catch (e: Exception) {
            server = null
            _isRunning.value = false
            _urls.value = emptyList()
            false
        }
    }

    fun stop() {
        try {
            server?.stop()
        } catch (_: Exception) {
        }
        server = null
        _isRunning.value = false
        _urls.value = emptyList()
    }

    fun setEntries(entries: List<SubstitutionEntry>, updatedAt: Long) {
        this.updatedAt = updatedAt
        parsedEntries = entries
    }

    fun setArchiveJson(json: String) {
        parsedEntries = try {
            gson.fromJson<List<SubstitutionEntry>>(json, entriesType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setSettings(roomFirst: Boolean, sortPeriod: Boolean, theme: Int, dynamic: Boolean) {
        isRoomFirst = roomFirst
        sortByPeriod = sortPeriod
        themeIndex = theme
        dynamicColor = dynamic
    }

    private fun computeLocalUrls(): List<String> {
        val result = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in nif.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        result.add("http://${addr.hostAddress}:$PORT")
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result.distinct()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun currentScheme(context: Context, dark: Boolean): ColorScheme {
        return if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            SeedPalettes[themeIndex.coerceIn(SeedPalettes.indices)].scheme(dark)
        }
    }

    private fun Color.toHex(): String = "#%06X".format(Locale.US, toArgb() and 0xFFFFFF)

    private fun buildPayload(): String {
        val context = appContext ?: return "{}"
        val dark = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val key = "$dark|$themeIndex|$dynamicColor|$isRoomFirst|$sortByPeriod|$updatedAt|${System.identityHashCode(parsedEntries)}"
        if (key == cachedPayloadKey) return cachedPayload ?: "{}"
        val payload = computePayload(dark)
        cachedPayload = payload
        cachedPayloadKey = key
        return payload
    }

    private fun computePayload(dark: Boolean): String {
        val context = appContext ?: return "{}"
        val scheme = currentScheme(context, dark)
        val theme = linkedMapOf<String, Any>(
            "dark" to dark,
            "primary" to scheme.primary.toHex(),
            "onPrimary" to scheme.onPrimary.toHex(),
            "primaryContainer" to scheme.primaryContainer.toHex(),
            "onPrimaryContainer" to scheme.onPrimaryContainer.toHex(),
            "secondary" to scheme.secondary.toHex(),
            "secondaryContainer" to scheme.secondaryContainer.toHex(),
            "onSecondaryContainer" to scheme.onSecondaryContainer.toHex(),
            "tertiary" to scheme.tertiary.toHex(),
            "tertiaryContainer" to scheme.tertiaryContainer.toHex(),
            "onTertiaryContainer" to scheme.onTertiaryContainer.toHex(),
            "background" to scheme.background.toHex(),
            "surface" to scheme.surface.toHex(),
            "surfaceDim" to scheme.surfaceDim.toHex(),
            "surfaceContainerLowest" to scheme.surfaceContainerLowest.toHex(),
            "surfaceContainerLow" to scheme.surfaceContainerLow.toHex(),
            "surfaceContainer" to scheme.surfaceContainer.toHex(),
            "surfaceContainerHigh" to scheme.surfaceContainerHigh.toHex(),
            "surfaceContainerHighest" to scheme.surfaceContainerHighest.toHex(),
            "onSurface" to scheme.onSurface.toHex(),
            "onSurfaceVariant" to scheme.onSurfaceVariant.toHex(),
            "surfaceVariant" to scheme.surfaceVariant.toHex(),
            "outline" to scheme.outline.toHex(),
            "outlineVariant" to scheme.outlineVariant.toHex(),
            "error" to scheme.error.toHex(),
            "errorContainer" to scheme.errorContainer.toHex(),
            "onErrorContainer" to scheme.onErrorContainer.toHex(),
            "inverseSurface" to scheme.inverseSurface.toHex(),
            "inverseOnSurface" to scheme.inverseOnSurface.toHex()
        )
        val payload = linkedMapOf<String, Any>(
            "entries" to parsedEntries,
            "theme" to theme,
            "isRoomFirst" to isRoomFirst,
            "sortByPeriod" to sortByPeriod,
            "updatedAt" to updatedAt
        )
        return gson.toJson(payload)
    }

    private fun pageHtml(): String {
        cachedPage?.let { return it }
        val ctx = appContext ?: return ""
        return try {
            ctx.assets.open("webserver/index.html").bufferedReader().use { it.readText() }
                .also { cachedPage = it }
        } catch (_: Exception) {
            "<html><head><meta charset=\"utf-8\"></head><body><h1>Astra</h1></body></html>"
        }
    }

    private class DSBWebServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val response = when (session.uri) {
                "/", "/index.html" ->
                    NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_HTML, LocalWebServer.pageHtml())
                "/api/data" ->
                    NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_JSON, LocalWebServer.buildPayload())
                "/favicon.ico" ->
                    NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "")
                else ->
                    NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
            }
            response.addHeader(REFRESH_HEADER, "*")
            response.addHeader("Cache-Control", "no-store")
            return response
        }
    }
}
