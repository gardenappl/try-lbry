package ua.gardenapple.trylbry

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedInputStream
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {
    companion object {
        private const val LBRY_COM_API = "https://api.lbry.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        runBlocking(Dispatchers.IO) {
            val lbryChannel = resolveYoutube("PC2uCxX7ISc", true)
            val lbryUri = Uri.parse("https://open.lbry.com/${lbryChannel.replace('#', ':')}")
            val intent = Intent(Intent.ACTION_VIEW, lbryUri)
            startActivity(intent)
            finish()
        }
    }

    private suspend fun resolveYoutube(id: String, isVideo: Boolean = true): String {
        val queryParameter = if (isVideo)
            "video_ids"
        else
            "channel_ids"

        val apiUrl = URL("$LBRY_COM_API/yt/resolve?$queryParameter=${URLEncoder.encode(id, "UTF-8")}")

        return withContext(Dispatchers.IO) {
            val connection = apiUrl.openConnection() as HttpsURLConnection
            val responseString = connection.inputStream.reader().use {
                it.readText()
            }

            val responseJson = JSONObject(responseString)
            if (!responseJson.getBoolean("success")) {
                throw RuntimeException(responseJson.getString("error"))
            }

            responseJson.getJSONObject("data")
                .getJSONObject(if (isVideo) "videos" else "channels")
                .getString(id)
        }
    }
}