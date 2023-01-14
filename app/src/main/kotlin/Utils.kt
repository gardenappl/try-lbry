package garden.appl.trylbry

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

object Utils {
    fun lbryNameToOdyseeUri(lbryName: String): Uri {
        return Uri.parse("https://odysee.com/${lbryName.replace('#', ':')}")
    }

    fun lbryNameToUri(lbryName: String): Uri {
        return Uri.parse("lbry://$lbryName")
    }

    fun lbryUriToName(lbry: Uri): String {
        return lbry.toString().removePrefix("lbry://")
    }

    fun queryIntentActivities(packageManager: PackageManager, intent: Intent): List<ResolveInfo> {
        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            packageManager.queryIntentActivities(intent,
                PackageManager.ResolveInfoFlags.of(0.toLong()))
        else
            packageManager.queryIntentActivities(intent, 0)
    }

    fun queryDefaultIntentActivities(packageManager: PackageManager, intent: Intent): List<ResolveInfo> {
        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            packageManager.queryIntentActivities(intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        else
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
}