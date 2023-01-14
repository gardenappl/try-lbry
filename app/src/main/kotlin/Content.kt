package garden.appl.trylbry

import android.net.Uri

data class Content(val type: ContentType, val originalUrl: String, var id: String? = null) {
    /**
     * Note: NewPipe does not support vnd.youtube for channels
     */
    fun toVendorUri(): Uri? {
        return when (type) {
            ContentType.VIDEO -> Uri.parse("vnd.youtube://$id")
//            else -> Uri.parse("vnd.youtube:${Uri.parse(originalUrl).schemeSpecificPart}")
            else -> null
        }
    }
}