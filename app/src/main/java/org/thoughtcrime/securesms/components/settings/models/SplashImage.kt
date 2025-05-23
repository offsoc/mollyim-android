package org.thoughtcrime.securesms.components.settings.models

import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.settings.PreferenceModel
import org.thoughtcrime.securesms.util.ThemeUtil
import org.thoughtcrime.securesms.util.adapter.mapping.LayoutFactory
import org.thoughtcrime.securesms.util.adapter.mapping.MappingAdapter
import org.thoughtcrime.securesms.util.adapter.mapping.MappingViewHolder

/**
 * Renders a single image, horizontally centered.
 */
object SplashImage {

  fun register(mappingAdapter: MappingAdapter) {
    mappingAdapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.splash_image))
  }

  class Model(@DrawableRes val splashImageResId: Int, val splashImageTintResId: Int = -1) : PreferenceModel<Model>() {
    override fun areItemsTheSame(newItem: Model): Boolean {
      return newItem.splashImageResId == splashImageResId && newItem.splashImageTintResId == splashImageTintResId
    }
  }

  private class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {

    private val splashImageView: ImageView = itemView as ImageView

    override fun bind(model: Model) {
      splashImageView.setImageResource(model.splashImageResId)

      if (model.splashImageTintResId != -1) {
        splashImageView.setColorFilter(ThemeUtil.getThemedColor(context, model.splashImageTintResId))
      } else {
        splashImageView.clearColorFilter()
      }
    }
  }
}
