package org.thoughtcrime.securesms.stories.tabs

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.signal.core.util.DimensionUnit
import org.signal.core.util.concurrent.LifecycleDisposable
import org.signal.core.util.dp
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.ViewBinderDelegate
import org.thoughtcrime.securesms.databinding.ConversationListTabsBinding
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.stories.Stories
import org.thoughtcrime.securesms.util.TextSecurePreferences
import org.thoughtcrime.securesms.util.visible

/**
 * Displays the "Chats" and "Stories" tab to a user.
 */
class ConversationListTabsFragment : Fragment(R.layout.conversation_list_tabs) {

  private val viewModel: ConversationListTabsViewModel by viewModels(ownerProducer = { requireActivity() })
  private val disposables: LifecycleDisposable = LifecycleDisposable()
  private val binding by ViewBinderDelegate(ConversationListTabsBinding::bind)
  private var shouldBeImmediate = true
  private var pillAnimator: Animator? = null

  private val largeConstraintSet: ConstraintSet = ConstraintSet()
  private val smallConstraintSet: ConstraintSet = ConstraintSet()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    disposables.bindTo(viewLifecycleOwner)

    val iconTint = ContextCompat.getColor(requireContext(), R.color.signal_colorOnSecondaryContainer)

    largeConstraintSet.clone(binding.root)
    smallConstraintSet.clone(requireContext(), R.layout.conversation_list_tabs_small)

    binding.chatsTabIcon.setTintColor(iconTint)
    binding.callsTabIcon.setTintColor(iconTint)
    binding.storiesTabIcon.setTintColor(iconTint)

    view.findViewById<View>(R.id.chats_tab_touch_point).setOnClickListener {
      viewModel.onChatsSelected()
    }

    view.findViewById<View>(R.id.calls_tab_touch_point).setOnClickListener {
      viewModel.onCallsSelected()
    }

    view.findViewById<View>(R.id.stories_tab_touch_point).setOnClickListener {
      viewModel.onStoriesSelected()
    }

    updateTabsVisibility()

    disposables += viewModel.state.subscribeBy {
      update(it, shouldBeImmediate)
      shouldBeImmediate = false
    }
  }

  private fun LottieAnimationView.setTintColor(color: Int) {
    if (composition == null) {
      addLottieOnCompositionLoadedListener { applyTint(color) }
    } else {
      applyTint(color)
    }
  }

  private fun LottieAnimationView.applyTint(color: Int) {
    addValueCallback(
      KeyPath("**"),
      LottieProperty.COLOR
    ) { color }
  }

  override fun onResume() {
    super.onResume()
    updateTabsVisibility()
  }

  private fun updateTabsVisibility() {
    if (SignalStore.settings.useCompactNavigationBar) {
      smallConstraintSet.applyTo(binding.root)
      binding.root.minHeight = 48.dp
    } else {
      largeConstraintSet.applyTo(binding.root)
      binding.root.minHeight = 80.dp
    }

    listOf(
      binding.callsPill,
      binding.callsTabIcon,
      binding.callsTabContainer,
      binding.callsTabLabel,
      binding.callsUnreadIndicator,
      binding.callsTabTouchPoint
    ).forEach {
      it.visible = TextSecurePreferences.getNavbarShowCalls(requireContext())
    }

    listOf(
      binding.storiesPill,
      binding.storiesTabIcon,
      binding.storiesTabContainer,
      binding.storiesTabLabel,
      binding.storiesUnreadIndicator,
      binding.storiesTabTouchPoint
    ).forEach {
      it.visible = Stories.isFeatureEnabled()
    }

    if (SignalStore.settings.useCompactNavigationBar) {
      listOf(
        binding.callsTabLabel,
        binding.chatsTabLabel,
        binding.storiesTabLabel
      ).forEach {
        it.visible = false
      }
    }

    update(viewModel.stateSnapshot, true)
  }

  private fun update(state: ConversationListTabsState, immediate: Boolean) {
    val storiesEnabled = Stories.isFeatureEnabled()
    val showCalls = TextSecurePreferences.getNavbarShowCalls(requireContext())

    binding.chatsTabIcon.isSelected = state.tab == ConversationListTab.CHATS
    binding.chatsPill.isSelected = state.tab == ConversationListTab.CHATS

    if (storiesEnabled) {
      binding.storiesTabIcon.isSelected = state.tab == ConversationListTab.STORIES
      binding.storiesPill.isSelected = state.tab == ConversationListTab.STORIES
    }

    if (showCalls) {
      binding.callsTabIcon.isSelected = state.tab == ConversationListTab.CALLS
      binding.callsPill.isSelected = state.tab == ConversationListTab.CALLS
    }

    val hasStateChange = state.tab != state.prevTab
    if (immediate) {
      binding.chatsTabIcon.pauseAnimation()
      binding.chatsTabIcon.progress = if (state.tab == ConversationListTab.CHATS) 1f else 0f

      if (storiesEnabled) {
        binding.storiesTabIcon.pauseAnimation()
        binding.storiesTabIcon.progress = if (state.tab == ConversationListTab.STORIES) 1f else 0f
      }

      if (showCalls) {
        binding.callsTabIcon.pauseAnimation()
        binding.callsTabIcon.progress = if (state.tab == ConversationListTab.CALLS) 1f else 0f
      }

      runPillAnimation(
        0,
        listOfNotNull(
          binding.chatsPill,
          binding.callsPill.takeIf { showCalls },
          binding.storiesPill.takeIf { storiesEnabled }
        )
      )
    } else if (hasStateChange) {
      runLottieAnimations(
        listOfNotNull(
          binding.chatsTabIcon,
          binding.callsTabIcon.takeIf { showCalls },
          binding.storiesTabIcon.takeIf { storiesEnabled }
        )
      )

      runPillAnimation(
        150,
        listOfNotNull(
          binding.chatsPill,
          binding.callsPill.takeIf { showCalls },
          binding.storiesPill.takeIf { storiesEnabled }
        )
      )
    }

    binding.chatsUnreadIndicator.visible = state.unreadMessagesCount > 0
    binding.chatsUnreadIndicator.text = formatCount(state.unreadMessagesCount)

    if (storiesEnabled) {
      binding.storiesUnreadIndicator.visible = state.unreadStoriesCount > 0 || state.hasFailedStory
      binding.storiesUnreadIndicator.text = if (state.hasFailedStory) "!" else formatCount(state.unreadStoriesCount)
    }

    if (showCalls) {
      binding.callsUnreadIndicator.visible = state.unreadCallsCount > 0
      binding.callsUnreadIndicator.text = formatCount(state.unreadCallsCount)
    }

    requireView().visible = state.visibilityState.isVisible()
  }

  private fun runLottieAnimations(toAnimate: List<LottieAnimationView>) {
    toAnimate.forEach {
      if (it.isSelected) {
        it.resumeAnimation()
      } else {
        if (it.isAnimating) {
          it.pauseAnimation()
        }

        it.progress = 0f
      }
    }
  }

  private fun runPillAnimation(duration: Long, toAnimate: List<ImageView>) {
    val (selected, unselected) = toAnimate.partition { it.isSelected }

    pillAnimator?.cancel()
    pillAnimator = AnimatorSet().apply {
      this.duration = duration
      interpolator = PathInterpolatorCompat.create(0.17f, 0.17f, 0f, 1f)
      playTogether(
        selected.map { view ->
          view.visibility = View.VISIBLE
          ValueAnimator.ofInt(view.paddingLeft, 0).apply {
            addUpdateListener {
              view.setPadding(it.animatedValue as Int, 0, it.animatedValue as Int, 0)
            }
          }
        }
      )
      start()
    }

    unselected.forEach {
      val smallPad = DimensionUnit.DP.toPixels(16f).toInt()
      it.setPadding(smallPad, 0, smallPad, 0)
      it.visibility = View.INVISIBLE
    }
  }

  private fun formatCount(count: Long): String {
    if (count > 99L) {
      return getString(R.string.ConversationListTabs__99p)
    }
    return count.toString()
  }
}
