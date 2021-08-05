package ua.gardenapple.trylbry

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.webkit.URLUtil
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import ua.gardenapple.trylbry.databinding.DialogMainBinding

class DialogActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    companion object {
        private const val LOGGING_TAG = "LbryCheckDialog"
    }

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
        val content = LbryYoutubeChecker.getContentQuick(youtubeUrlString)
//        Log.d(LOGGING_TAG, "Content: $content")

        if (content == null) {
            showDialogInvalidUrl(youtubeUrlString)
            return
        }

        binding.message.text = resources.getString(
            when (content.type) {
                ContentType.VIDEO -> R.string.dialog_checking_youtube_video
                ContentType.CHANNEL -> R.string.dialog_checking_youtube_channel
            }
        )
        binding.watchOnYoutube.setOnClickListener {
            ContentIntents.startYoutubeActivity(this, youtubeUri, false)
            finish()
        }

        launch {
            try {
                val lbryUrlString = LbryYoutubeChecker.getLbryUrl(content)

                if (lbryUrlString == null) {
                    ContentIntents.startYoutubeActivity(this@DialogActivity, youtubeUri, true)
                    finish()
                    return@launch
                } else {
                    binding.watchOnLbry.setOnClickListener {
                        ContentIntents.startLbryActivity(this@DialogActivity, lbryUrlString)
                        finish()
                    }

                    binding.watchOnLbry.visibility = View.VISIBLE
                    binding.message.text = resources.getString(when (content.type) {
                        ContentType.VIDEO -> R.string.dialog_found_youtube_video
                        ContentType.CHANNEL -> R.string.dialog_found_youtube_channel
                    })
                    binding.progressBar.visibility = View.GONE
                }
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