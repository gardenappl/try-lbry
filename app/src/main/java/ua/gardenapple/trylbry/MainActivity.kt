package ua.gardenapple.trylbry

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import ua.gardenapple.trylbry.databinding.DialogMainBinding
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    companion object {
        private const val LBRY_COM_API = "https://api.lbry.com"
        private const val OPEN_LBRY_COM = "https://open.lbry.com"

        private val videoIdPattern = Regex("""[/?]v[=/]([^&/?]*)""")
        private val videoIdPatternShort = Regex("""https?://youtu\.be/([^?/]*)""")
        private val channelIdPattern = Regex("""/channel/([^?/]*)""")
    }
    
    private enum class ContentType {
        VIDEO, CHANNEL
    }
    
    private lateinit var lbryUri: Uri
    private lateinit var binding: DialogMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = DialogMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val youtubeUrl = intent!!.data.toString()
        val id: String
        val contentType: ContentType
        
        when {
            videoIdPattern.containsMatchIn(youtubeUrl) -> {
                id = videoIdPattern.find(youtubeUrl)!!.groupValues[1]
                contentType = ContentType.VIDEO
            }
            videoIdPatternShort.containsMatchIn(youtubeUrl) -> {
                id = videoIdPatternShort.find(youtubeUrl)!!.groupValues[1]
                contentType = ContentType.VIDEO
            }
            channelIdPattern.containsMatchIn(youtubeUrl) -> {
                id = channelIdPattern.find(youtubeUrl)!!.groupValues[1]
                contentType = ContentType.CHANNEL
            }
            else -> throw IllegalArgumentException("Could not get content ID from YouTube URL")
        }

        binding.message.text = resources.getString(
                when(contentType) {
                    ContentType.VIDEO -> R.string.dialog_checking_youtube_video
                    ContentType.CHANNEL -> R.string.dialog_checking_youtube_channel
                }
        )
        binding.watchOnLbry.setOnClickListener {
            val viewIntent = Intent(Intent.ACTION_VIEW, lbryUri)
            startActivity(viewIntent)
            finish()
        }
        binding.watchOnYoutube.setOnClickListener {
            startYoutubeActivity()
            finish()
        }
        binding.cancelButton.setOnClickListener {
            finish()
        }
        
        launch {
            val lbryChannel = resolveYoutube(id, contentType)
            if (lbryChannel == null) {
                startYoutubeActivity()
                finish()
            } else {
                lbryUri = Uri.parse("$OPEN_LBRY_COM/${lbryChannel.replace('#', ':')}")

                binding.watchOnLbry.visibility = View.VISIBLE
                binding.message.text = resources.getString(when (contentType) {
                    ContentType.VIDEO -> R.string.dialog_found_youtube_video
                    ContentType.CHANNEL -> R.string.dialog_found_youtube_channel
                })
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }

    private suspend fun resolveYoutube(id: String, contentType: ContentType): String? {
        val queryParameter = when(contentType) {
            ContentType.VIDEO -> "video_ids"
            ContentType.CHANNEL -> "channel_ids"
        }

        val apiUrl = URL("$LBRY_COM_API/yt/resolve?$queryParameter=${URLEncoder.encode(id, "UTF-8")}")

        val responseString = withContext(Dispatchers.IO) {
            val connection = apiUrl.openConnection() as HttpsURLConnection
            connection.inputStream.reader().use {
                it.readText()
            }
        }

        val responseJson = JSONObject(responseString)
        if (!responseJson.getBoolean("success")) {
            throw RuntimeException(responseJson.getString("error"))
        }

        val resultJson = responseJson.getJSONObject("data")
                .getJSONObject(when(contentType) {
                    ContentType.VIDEO -> "videos"
                    ContentType.CHANNEL -> "channels"
                })

        return if (resultJson.isNull(id))
            null
        else
            resultJson.getString(id)
    }
    
    private fun startYoutubeActivity() {
        val intent = Intent(Intent.ACTION_VIEW, intent.data!!)
        val chooser = Intent.createChooser(intent, resources.getString(R.string.open_youtube))
        startActivity(chooser)
    }
}