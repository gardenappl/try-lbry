package garden.appl.trylbry

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import garden.appl.trylbry.databinding.MainActivityBinding

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    companion object {
        private const val LOGGING_TAG = "MainActivity"
    }

    private lateinit var binding: MainActivityBinding

    private fun setInputLayoutClipboardIcon() {
        binding.urlInputLayout.setEndIconDrawable(R.drawable.baseline_content_paste_24)

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        binding.urlInputLayout.setEndIconOnClickListener {
            val clipboardItem = clipboard.primaryClip?.getItemAt(0)
                ?: return@setEndIconOnClickListener

            val clipboardUriString = if (clipboardItem.text != null) {
                clipboardItem.text.toString()
            } else if (clipboardItem.uri != null
                && clipboardItem.uri.scheme?.startsWith("http") == true) {

                clipboardItem.uri.toString()
            } else {
                Toast.makeText(this, R.string.clipboard_bad, Toast.LENGTH_SHORT).show()
                return@setEndIconOnClickListener
            }

            val content = LbryYoutubeChecker.getContentQuick(clipboardUriString)
            if (content != null) {
                binding.urlInput.setText(clipboardUriString)
            } else {
                Toast.makeText(this, R.string.clipboard_bad, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setInputLayoutClipboardIcon()

        binding.urlInput.addTextChangedListener { editable ->
            val string = editable.toString().trim()
            if (string.isEmpty()) {
                setInputLayoutClipboardIcon()
                binding.urlInputLayout.error = null
                binding.progressBar.visibility = View.GONE
                binding.watchOnLbry.visibility = View.GONE
                binding.watchOnYoutube.visibility = View.GONE
                binding.message.setText(R.string.activity_desc)
                return@addTextChangedListener
            }
            binding.urlInputLayout.setEndIconDrawable(R.drawable.baseline_clear_24)
            binding.urlInputLayout.setEndIconOnClickListener {
                binding.urlInput.setText("")
            }

            val content = LbryYoutubeChecker.getContentQuick(string)

            if (content == null || !URLUtil.isValidUrl(string)) {
                binding.urlInputLayout.error = getString(R.string.activity_error_wrong_url)
                binding.progressBar.visibility = View.GONE
                binding.watchOnLbry.visibility = View.GONE
                binding.watchOnYoutube.visibility = View.GONE
                binding.message.setText(R.string.activity_desc)
                return@addTextChangedListener
            }

            binding.urlInputLayout.error = null
            binding.progressBar.visibility = View.VISIBLE
            binding.watchOnYoutube.setOnClickListener {
                ContentIntents.startYoutubeActivity(this@MainActivity, content, false)
            }
            binding.watchOnYoutube.visibility = View.VISIBLE
            binding.message.setText(
                when (content.type) {
                    ContentType.VIDEO -> R.string.dialog_checking_youtube_video
                    ContentType.CHANNEL -> R.string.dialog_checking_youtube_channel
                }
            )


            launch {
                try {
                    val lbryName = LbryYoutubeChecker.getLbryName(content)

                    binding.progressBar.visibility = View.GONE
                    if (lbryName == null) {
                        binding.watchOnLbry.visibility = View.GONE
                        binding.message.setText(R.string.youtube_not_found)
                    } else {
                        binding.watchOnLbry.setOnClickListener {
                            ContentIntents.startLbryActivity(this@MainActivity, lbryName)
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