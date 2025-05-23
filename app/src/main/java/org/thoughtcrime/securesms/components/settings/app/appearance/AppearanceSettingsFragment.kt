package org.thoughtcrime.securesms.components.settings.app.appearance

import android.os.Build
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import org.signal.core.util.concurrent.observe
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.settings.DSLConfiguration
import org.thoughtcrime.securesms.components.settings.DSLSettingsFragment
import org.thoughtcrime.securesms.components.settings.DSLSettingsText
import org.thoughtcrime.securesms.components.settings.app.appearance.navbar.ChooseNavigationBarStyleFragment
import org.thoughtcrime.securesms.components.settings.configure
import org.thoughtcrime.securesms.keyvalue.SettingsValues
import org.thoughtcrime.securesms.util.DynamicTheme
import org.thoughtcrime.securesms.util.adapter.mapping.MappingAdapter
import org.thoughtcrime.securesms.util.navigation.safeNavigate

class AppearanceSettingsFragment : DSLSettingsFragment(R.string.preferences__appearance) {

  private lateinit var viewModel: AppearanceSettingsViewModel

  private val themeLabels by lazy { resources.getStringArray(R.array.pref_theme_entries) }
  private val themeValues by lazy { resources.getStringArray(R.array.pref_theme_values) }

  private val messageFontSizeLabels by lazy { resources.getStringArray(R.array.pref_message_font_size_entries) }
  private val messageFontSizeValues by lazy { resources.getIntArray(R.array.pref_message_font_size_values) }

  private val languageLabels by lazy { resources.getStringArray(R.array.language_entries) }
  private val languageValues by lazy { resources.getStringArray(R.array.language_values) }

  override fun bindAdapter(adapter: MappingAdapter) {
    viewModel = ViewModelProvider(this)[AppearanceSettingsViewModel::class.java]

    viewModel.state.observe(viewLifecycleOwner) { state ->
      adapter.submitList(getConfiguration(state).toMappingModelList())
    }

    childFragmentManager.setFragmentResultListener(ChooseNavigationBarStyleFragment.REQUEST_KEY, viewLifecycleOwner) { key, bundle ->
      if (bundle.getBoolean(key, false)) {
        viewModel.refreshState()
      }
    }
  }

  private fun getConfiguration(state: AppearanceSettingsState): DSLConfiguration {
    return configure {
      radioListPref(
        title = DSLSettingsText.from(R.string.preferences__language),
        listItems = languageLabels,
        selected = languageValues.indexOf(state.language),
        onSelected = {
          viewModel.setLanguage(languageValues[it])
        }
      )

      radioListPref(
        title = DSLSettingsText.from(R.string.preferences__theme),
        listItems = themeLabels,
        selected = themeValues.indexOf(state.theme.serialize()),
        onSelected = {
          viewModel.setTheme(activity, SettingsValues.Theme.deserialize(themeValues[it]), state.dynamicColors)
        }
      )

      clickPref(
        title = DSLSettingsText.from(R.string.preferences__chat_color_and_wallpaper),
        onClick = {
          Navigation.findNavController(requireView()).safeNavigate(R.id.action_appearanceSettings_to_wallpaperActivity)
        }
      )

      if (DynamicTheme.isDynamicColorsAvailable()) {
        switchPref(
          title = DSLSettingsText.from(R.string.preferences_appearance__dynamic_colors),
          summary = DSLSettingsText.from(R.string.preferences_appearance__use_system_colors_for_the_app_theme),
          isChecked = state.dynamicColors,
          onClick = {
            viewModel.setTheme(activity, state.theme, !state.dynamicColors)
          }
        )
      }

      if (Build.VERSION.SDK_INT >= 26) {
        clickPref(
          title = DSLSettingsText.from(R.string.preferences__app_icon),
          onClick = {
            Navigation.findNavController(requireView()).safeNavigate(R.id.action_appearanceSettings_to_appIconActivity)
          }
        )
      }

      radioListPref(
        title = DSLSettingsText.from(R.string.preferences_chats__message_text_size),
        listItems = messageFontSizeLabels,
        selected = messageFontSizeValues.indexOf(state.messageFontSize),
        onSelected = {
          viewModel.setMessageFontSize(messageFontSizeValues[it].toInt())
        }
      )

      dividerPref()

      sectionHeaderPref(R.string.preferences_appearance__navigation_bar)

      clickPref(
        title = DSLSettingsText.from(R.string.preferences_navigation_bar_size),
        summary = DSLSettingsText.from(
          if (state.isCompactNavigationBar) {
            R.string.preferences_compact
          } else {
            R.string.preferences_normal
          }
        ),
        onClick = {
          ChooseNavigationBarStyleFragment().show(childFragmentManager, null)
        }
      )
    }
  }
}
