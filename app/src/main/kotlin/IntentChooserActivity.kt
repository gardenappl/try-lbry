package garden.appl.trylbry

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import garden.appl.trylbry.databinding.IntentChooserActivityBinding

class IntentChooserActivity : AppCompatActivity() {
    companion object {
        private const val LOGGING_TAG = "IntentChooserActivity"

        private const val EXTRA_PROMPT_MESSAGE = "garden.appl.EXTRA_PROMPT_MESSAGE"

        fun start(context: Context, uri: Uri, @StringRes promptMessage: Int) {
            val intent = Intent(Intent.ACTION_VIEW, uri, context,
                IntentChooserActivity::class.java)
            intent.putExtra(EXTRA_PROMPT_MESSAGE, promptMessage)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: IntentChooserActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = IntentChooserActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri = intent.data ?: throw IllegalArgumentException("URI not supplied!")
        val intentActivities = if (uri.scheme == "lbry") {
            val lbryName = Utils.lbryUriToName(uri)

            val lbryIntent = Intent(Intent.ACTION_VIEW, uri)
            val currentActivities = Utils.queryIntentActivities(packageManager, lbryIntent).toMutableList()

            val odyseeIntent = Intent(Intent.ACTION_VIEW, Utils.lbryNameToOdyseeUri(lbryName))
            currentActivities.addAll(Utils.queryIntentActivities(packageManager, odyseeIntent))

            currentActivities
        } else {
            val youtubeIntent = Intent(Intent.ACTION_VIEW, uri)
            val currentActivities = Utils.queryIntentActivities(packageManager, youtubeIntent)

            currentActivities.filter { info ->
                info.activityInfo.packageName != applicationContext.packageName
            }
        }

        for (info in intentActivities)
            Log.d(LOGGING_TAG, "${info.loadLabel(packageManager)}, default: ${info.isDefault}, filter: ${info.filter}")

        if (intentActivities.first().isDefault &&
                intentActivities.drop(1).none { info -> info.isDefault }) {
            startActivity(intentActivities.first().activityInfo)
            finish()
        } else {
            binding.message.setText(intent.extras?.getInt(EXTRA_PROMPT_MESSAGE) ?: -1)
            binding.intentList.adapter = IntentChooserAdapter(this, intentActivities)
            binding.intentList.setOnItemClickListener { parent, view, position, id ->
                startActivity(intentActivities[position].activityInfo)
            }
        }
    }

    private fun startActivity(activityInfo: ActivityInfo) {
        val intent = Intent(Intent.ACTION_VIEW, intent.data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.component = ComponentName.createRelative(activityInfo.packageName, activityInfo.name)
        } else {
            intent.setClassName(activityInfo.packageName, activityInfo.name)
        }
        startActivity(intent)
    }
}