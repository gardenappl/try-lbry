package garden.appl.trylbry

import android.content.Context
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import garden.appl.trylbry.databinding.IntentChooserWidgetBinding

class IntentChooserAdapter @JvmOverloads constructor(
    context: Context,
    objects: List<ResolveInfo>
) : ArrayAdapter<ResolveInfo>(context, R.layout.intent_chooser_widget, 0, objects) {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = convertView?.let {
            IntentChooserWidgetBinding.bind(it)
        } ?: IntentChooserWidgetBinding.inflate(inflater, parent, false)
        val resolveInfo = getItem(position)!!
        binding.intentIcon.setImageDrawable(resolveInfo.loadIcon(context.packageManager))
        binding.intentName.text = resolveInfo.loadLabel(context.packageManager)
        return binding.root
    }
}