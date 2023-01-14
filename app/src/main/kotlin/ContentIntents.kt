package garden.appl.trylbry

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

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
            content.toVendorUri()?.let { vendorUri ->
                context.startActivity(Intent(Intent.ACTION_VIEW, vendorUri))
                return@startYoutubeActivity
            }
        } catch (_: ActivityNotFoundException) {}

        val originalIntent = Intent(Intent.ACTION_VIEW, Uri.parse(content.originalUrl))
        var multipleResolutions = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val nonBrowserIntent = Intent(originalIntent)
            nonBrowserIntent.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)

            val resolutions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(nonBrowserIntent,
                    PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(nonBrowserIntent, 0)
            }
            if (resolutions.size > 1)
                multipleResolutions = true

            val resolution = if (resolutions.first().activityInfo.packageName != null
                && resolutions.first().activityInfo.packageName != context.packageName) {

                resolutions.first()
            } else {
                resolutions.removeIf { info ->
                    info.activityInfo.packageName == context.packageName
                }
                if (resolutions.size == 1)
                    resolutions.first()
                else
                    null
            }
            if (resolution != null) {
                nonBrowserIntent.component = ComponentName.createRelative(
                    resolution.activityInfo.packageName,
                    resolution.activityInfo.name
                )
                context.startActivity(nonBrowserIntent)
                return
            }
        } else {
            @Suppress("DEPRECATION") val resolutions =
                context.packageManager.queryIntentActivities(originalIntent, 0)
            if (resolutions.size > 1)
                multipleResolutions = true

            val resolution = if (resolutions.first().activityInfo.packageName != null
                && resolutions.first().activityInfo.packageName != context.packageName) {

                resolutions.first()
            } else {
                val foreignResolutions = resolutions.filter { info ->
                    info.activityInfo.packageName != context.packageName
                }
                if (foreignResolutions.size == 1)
                    foreignResolutions.first()
                else
                    null
            }
            if (resolution != null) {
                originalIntent.component = ComponentName(
                    resolution.activityInfo.packageName,
                    resolution.activityInfo.name
                )
                context.startActivity(originalIntent)
                return
            }
        }

        if (multipleResolutions) {
            val message = if (lbryNotFoundMessage)
                R.string.youtube_not_found
            else
                R.string.open_youtube
            val chooser = Intent.createChooser(originalIntent, context.getString(message))
            context.startActivity(chooser)
        } else {
            // Fallback to default browser

            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://"))
            val browserResolution = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.resolveActivity(browserIntent,
                    PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.resolveActivity(browserIntent, 0)
            }

            if (browserResolution != null) {
                originalIntent.component = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ComponentName.createRelative(
                        browserResolution.activityInfo.packageName,
                        browserResolution.activityInfo.name
                    )
                } else {
                    ComponentName(
                        browserResolution.activityInfo.packageName,
                        browserResolution.activityInfo.name
                    )
                }
                context.startActivity(originalIntent)
            } else {
                throw ActivityNotFoundException("No default browser?")
            }
        }
    }
}