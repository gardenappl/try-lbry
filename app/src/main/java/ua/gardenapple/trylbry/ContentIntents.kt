package ua.gardenapple.trylbry

import android.content.Context
import android.content.Intent
import android.net.Uri

object ContentIntents {
    private const val OPEN_LBRY_COM = "https://open.lbry.com"

    fun startLbryActivity(context: Context, lbryUrl: String) {
        val uri = Uri.parse("$OPEN_LBRY_COM/${lbryUrl.replace('#', ':')}")

        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    fun startYoutubeActivity(context: Context, youtubeUrl: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, youtubeUrl)
        val chooser = Intent.createChooser(intent, context.getString(R.string.open_youtube))
        context.startActivity(chooser)
    }
}