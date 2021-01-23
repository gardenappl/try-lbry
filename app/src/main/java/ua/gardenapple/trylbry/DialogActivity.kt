package ua.gardenapple.trylbry

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.URLUtil
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import ua.gardenapple.trylbry.databinding.DialogMainBinding
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import javax.net.ssl.HttpsURLConnection

class DialogActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    companion object {
        private const val LOGGING_TAG = "LbryCheckDialog"
        
        private const val LBRY_COM_API = "https://api.lbry.com"
        private const val OPEN_LBRY_COM = "https://open.lbry.com"

        private val videoIdPattern = Regex("""[/?]v[=/]([^&/?]*)""")
        private val videoIdPatternShort = Regex("""https?://youtu\.be/([^?/]*)""")
        private val channelIdPattern = Regex("""/channel/([^?/]*)""")
        private val channelNamePattern = Regex("""/(?:c|user)/([^?/]*)""")
        
        private val htmlChannelIdPattern =
                Regex("""(?:"|\\x22)externalId(?:"|\\x22):(?:"|\\x22)(.*?)(?:"|\\x22)""")
    }
    
    private enum class ContentType {
        VIDEO, CHANNEL
    }

    private lateinit var lbryUri: Uri
    private lateinit var binding: DialogMainBinding

    private var lbryCheckDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DialogMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cancelButton.setOnClickListener {
            finish()
        }

        val youtubeUri: Uri
        val youtubeUrlString: String

        when (intent.action) {
            Intent.ACTION_SEND -> {
                youtubeUrlString = intent.getStringExtra(Intent.EXTRA_TEXT)!!.trim()
                if (!URLUtil.isValidUrl(youtubeUrlString)) {
                    showDialogInvalidUrl(youtubeUrlString)
                    return
                }
                youtubeUri = Uri.parse(youtubeUrlString)
            }
            Intent.ACTION_VIEW -> {
                youtubeUri = intent.data!!
                youtubeUrlString = youtubeUri.toString()
            }
            else -> throw IllegalArgumentException("Can't handle intent")
        }

        var id: String
        val contentType: ContentType
        
        when {
            videoIdPattern.containsMatchIn(youtubeUrlString) -> {
                id = videoIdPattern.find(youtubeUrlString)!!.groupValues[1]
                contentType = ContentType.VIDEO
            }
            videoIdPatternShort.containsMatchIn(youtubeUrlString) -> {
                id = videoIdPatternShort.find(youtubeUrlString)!!.groupValues[1]
                contentType = ContentType.VIDEO
            }
            channelIdPattern.containsMatchIn(youtubeUrlString) -> {
                id = channelIdPattern.find(youtubeUrlString)!!.groupValues[1]
                contentType = ContentType.CHANNEL
            }
            channelNamePattern.containsMatchIn(youtubeUrlString) -> {
                id = ""
                contentType = ContentType.CHANNEL
            }
            else -> {
                showDialogInvalidUrl(youtubeUrlString)
                return
            }
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
            startYoutubeActivity(youtubeUri)
            finish()
        }

        launch {
            try {
                if (id.isEmpty())
                    id = getChannelId(URL(youtubeUrlString))

                val lbryUrlString = resolveYoutube(id, contentType)

                if (lbryUrlString == null) {
                    startYoutubeActivity(youtubeUri)
                    finish()
                    return@launch
                } else {
                    lbryUri = Uri.parse(
                            "$OPEN_LBRY_COM/${lbryUrlString.replace('#', ':')}"
                    )

                    binding.watchOnLbry.visibility = View.VISIBLE
                    binding.message.text = resources.getString(when (contentType) {
                        ContentType.VIDEO -> R.string.dialog_found_youtube_video
                        ContentType.CHANNEL -> R.string.dialog_found_youtube_channel
                    })
                    binding.progressBar.visibility = View.GONE
                }
//            } catch (e: UnknownHostException) {
//                //no internet, let YouTube app show an error
//                startYoutubeActivity(youtubeUri)
//                finish()
            } catch (e: Exception) {
                Log.e(LOGGING_TAG, "Error while checking LBRY availability")
                Log.e(LOGGING_TAG, e.localizedMessage ?: "no message")
                Log.e(LOGGING_TAG, e.stackTraceToString())

                binding.message.text = resources.getString(R.string.dialog_error)
                binding.progressBar.visibility = View.GONE
            } finally {
                lbryCheckDone = true
                showDialog()
            }
        }

        launch {
//            delay(1000)
            if (!lbryCheckDone)
                showDialog()
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
    
    private suspend fun getChannelId(channelUrl: URL): String {
        val channelHtml = withContext(Dispatchers.IO) {
            val connection = channelUrl.openConnection() as HttpsURLConnection
            connection.inputStream.reader().use {
                it.readText()
            }
        }
        //YouTube HTML is fucking gigantic (about 30 times heavier than Invidious),
        //using a regex on the whole document is a bad idea
        var startPos = channelHtml.indexOf("\"externalId\"") - 10
        if (startPos < 0)
            startPos = channelHtml.indexOf("\\x22externalId\\x22") - 10
        val match = htmlChannelIdPattern.find(channelHtml, startPos)
        return match!!.groupValues[1]
    }

    private fun startYoutubeActivity(youtubeUrl: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, youtubeUrl)
        val chooser = Intent.createChooser(intent, resources.getString(R.string.open_youtube))
        startActivity(chooser)
    }
    
    private fun showDialog() {
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        //Not sure why I have to manually set the dim amount
        val dimAmount = TypedValue()
        theme.resolveAttribute(android.R.attr.backgroundDimAmount, dimAmount, true)
        window.setDimAmount(dimAmount.float)

        binding.root.visibility = View.VISIBLE
    }
    
    private fun showDialogInvalidUrl(urlString: String) {
        binding.message.text = resources.getString(R.string.dialog_error_wrong_url, urlString)
        binding.progressBar.visibility = View.GONE
        binding.watchOnYoutube.visibility = View.GONE

        showDialog()
    }
}