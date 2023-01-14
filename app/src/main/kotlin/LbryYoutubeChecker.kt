package garden.appl.trylbry

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

object LbryYoutubeChecker {
    private val videoIdPattern = Regex("""(?:youtu\.be/|/embed/|/v/|/shorts/|\?v=)([^&/?]*)""")
    private val channelIdPattern = Regex("""/channel/([^?/]*)""")
    private val channelNamePattern = Regex("""/(?:c|user)/([^?/]*)""")

    private val htmlChannelIdPattern =
        Regex("""(?:"|\\x22)externalId(?:"|\\x22):(?:"|\\x22)(.*?)(?:"|\\x22)""")

    private const val API_URL = "https://api.lbry.com/yt/"


    fun getContentQuick(youtubeUrl: String): Content? {
        videoIdPattern.find(youtubeUrl)?.let { match ->
            return@getContentQuick Content(ContentType.VIDEO, youtubeUrl, match.groupValues[1])
        }
        channelIdPattern.find(youtubeUrl)?.let { match ->
            return@getContentQuick Content(ContentType.CHANNEL, youtubeUrl, match.groupValues[1])
        }
        //We don't know the ID without doing some parsing...
        if (channelNamePattern.containsMatchIn(youtubeUrl))
            return Content(ContentType.CHANNEL, youtubeUrl)

        return null
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

    suspend fun getLbryName(content: Content): String? {
        if (content.id == null && channelNamePattern.containsMatchIn(content.originalUrl))
            content.id = getChannelId(URL(content.originalUrl))

        val queryParameter = when (content.type) {
            ContentType.VIDEO -> "video_ids"
            ContentType.CHANNEL -> "channel_ids"
        }

        val apiUrl = URL("$API_URL/resolve?$queryParameter=${URLEncoder.encode(content.id, "UTF-8")}")

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
            .getJSONObject(when (content.type) {
                ContentType.VIDEO -> "videos"
                ContentType.CHANNEL -> "channels"
            })

        if (resultJson.isNull(content.id))
            return null
        return resultJson.getString(content.id)
    }
}