package ua.gardenapple.trylbry

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.URLUtil
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ua.gardenapple.trylbry.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    companion object {
        private const val LOGGING_TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.urlInput.addTextChangedListener { editable ->
            val string = editable.toString().trim()
            launch {
                if (string.isEmpty()) {
                    binding.urlInputLayout.error = null
                    binding.progressBar.visibility = View.GONE
                    binding.watchOnLbry.visibility = View.GONE
                    binding.watchOnYoutube.visibility = View.GONE
                    binding.message.setText(R.string.activity_desc)
                    return@launch
                }

                val content = LbryYoutubeChecker.getContentQuick(string)

                if (content == null || !URLUtil.isValidUrl(string)) {
                    binding.urlInputLayout.error = getString(R.string.activity_error_wrong_url)
                    binding.progressBar.visibility = View.GONE
                    binding.watchOnLbry.visibility = View.GONE
                    binding.watchOnYoutube.visibility = View.GONE
                    binding.message.setText(R.string.activity_desc)
                    return@launch
                }

                val youtubeUri = Uri.parse(string)

                binding.urlInputLayout.error = null
                binding.progressBar.visibility = View.VISIBLE
                binding.watchOnYoutube.setOnClickListener {
                    ContentIntents.startYoutubeActivity(this@MainActivity, youtubeUri, false)
                }
                binding.watchOnYoutube.visibility = View.VISIBLE
                binding.message.setText(when (content.type) {
                    ContentType.VIDEO -> R.string.dialog_checking_youtube_video
                    ContentType.CHANNEL -> R.string.dialog_checking_youtube_channel
                })


                try {
                    val lbryUrl = LbryYoutubeChecker.getLbryUrl(content)

                    binding.progressBar.visibility = View.GONE
                    if (lbryUrl == null) {
                        binding.watchOnLbry.visibility = View.GONE
                        binding.message.setText(R.string.youtube_not_found)
                    } else {
                        binding.watchOnLbry.setOnClickListener {
                            ContentIntents.startLbryActivity(this@MainActivity, lbryUrl)
                        }
                        binding.watchOnLbry.visibility = View.VISIBLE

                        binding.message.setText(when (content.type) {
                            ContentType.VIDEO -> R.string.dialog_found_youtube_video
                            ContentType.CHANNEL -> R.string.dialog_found_youtube_channel
                        })
                    }
                } catch (e: Exception) {
                    Log.e(LOGGING_TAG, "Error while checking LBRY availability")
                    Log.e(LOGGING_TAG, e.localizedMessage ?: "no message")
                    Log.e(LOGGING_TAG, e.stackTraceToString())

                    binding.progressBar.visibility = View.GONE
                    binding.watchOnLbry.visibility = View.GONE
                    binding.message.setText(R.string.dialog_error)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(R.string.help_what_is_lbry).apply {
            setIcon(R.drawable.ic_baseline_help_24)
            setOnMenuItemClickListener click@{
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lbry.com/"))
                startActivity(intent)
                return@click true
            }
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        return true
    }
}