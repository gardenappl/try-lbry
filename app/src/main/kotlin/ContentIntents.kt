package garden.appl.trylbry

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object ContentIntents {
    private fun lbryNameToOdyseeUri(lbryName: String): Uri {
        return Uri.parse("https://odysee.com/${lbryName.replace('#', ':')}")
    }

    private fun lbryNameToUri(lbryName: String): Uri {
        return Uri.parse("lbry://$lbryName")
    }

    private fun lbryUriToName(lbry: Uri): String {
        return lbry.toString().removePrefix("lbry://")
    }

    fun startLbryActivity(context: Context, lbryName: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, lbryNameToUri(lbryName)))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, lbryNameToOdyseeUri(lbryName)))
        }
    }

    fun startYoutubeActivity(context: Context, content: Content, lbryNotFoundMessage: Boolean) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, content.toVendorUri()))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(content.originalUrl)))
        }
    }
}