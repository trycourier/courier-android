package com.courier.android.ui.inbox

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.R
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.InboxAction
import com.courier.android.models.InboxMessage
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.clickMessage
import com.courier.android.ui.bar.CourierBar
import com.courier.android.utils.isDarkMode
import com.courier.android.utils.log
import com.courier.android.utils.setCourierFont
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CourierInbox(
    modifier: Modifier = Modifier,
    canSwipePages: Boolean = false,
    lightTheme: CourierInboxTheme = CourierInboxTheme(),
    darkTheme: CourierInboxTheme = CourierInboxTheme(),
    onClickMessageListener: ((message: InboxMessage, index: Int) -> Unit)? = null,
    onLongPressMessageListener: ((message: InboxMessage, index: Int) -> Unit)? = null,
    onClickActionListener: ((action: InboxAction, message: InboxMessage, index: Int) -> Unit)? = null,
    onScrollInboxListener: ((offsetInDp: Int) -> Unit)? = null
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CourierInbox(ctx)
        },
        update = { view ->
            view.canSwipePages = canSwipePages
            view.lightTheme = lightTheme
            view.darkTheme = darkTheme
            view.setOnClickMessageListener(onClickMessageListener)
            view.setOnLongPressMessageListener(onLongPressMessageListener)
            view.setOnClickActionListener(onClickActionListener)
            view.setOnScrollInboxListener(onScrollInboxListener)
        }
    )
}

open class CourierInbox @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    internal data class Page(
        val title: String,
        var tab: View? = null,
        val list: InboxListView
    )

    var lightTheme: CourierInboxTheme = CourierInboxTheme.DEFAULT_LIGHT
        set(value) {
            if (field != value) {
                field = value
                refreshTheme()
            }
        }

    var darkTheme: CourierInboxTheme = CourierInboxTheme.DEFAULT_DARK
        set(value) {
            if (field != value) {
                field = value
                refreshTheme()
            }
        }

    private var theme: CourierInboxTheme = CourierInboxTheme.DEFAULT_LIGHT
        @SuppressLint("NotifyDataSetChanged")
        set(value) {

            // Update the theme
            field = value

            reloadViews()

        }

    var canSwipePages = false
        set(value) {
            field = value
            enableSwipe(value)
        }

    private fun enableSwipe(enabled: Boolean) {
        viewPager.isUserInputEnabled = enabled
        pages.firstOrNull()?.list?.setItemGesturesEnabled(!enabled)
    }

    private var onClickInboxMessageAtIndex: ((InboxMessage, Int) -> Unit)? = null
    private var onLongPressInboxMessageAtIndex: ((InboxMessage, Int) -> Unit)? = null
    private var onClickInboxActionForMessageAtIndex: ((InboxAction, InboxMessage, Int) -> Unit)? = null
    private var onScrollInbox: ((Int) -> Unit)? = null

    fun setOnClickMessageListener(listener: ((message: InboxMessage, index: Int) -> Unit)?) {
        onClickInboxMessageAtIndex = listener
    }

    fun setOnLongPressMessageListener(listener: ((message: InboxMessage, index: Int) -> Unit)?) {
        onLongPressInboxMessageAtIndex = listener
    }

    fun setOnClickActionListener(listener: ((action: InboxAction, message: InboxMessage, index: Int) -> Unit)?) {
        onClickInboxActionForMessageAtIndex = listener
    }

    fun setOnScrollInboxListener(listener: ((offsetInDp: Int) -> Unit)?) {
        onScrollInbox = listener
    }

    private fun makeListView(feed: InboxMessageFeed, context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): InboxListView {
        return InboxListView(context, attrs, defStyleAttr, feed, inbox = this).apply {
            setOnClickMessageListener { message, index ->

                // Click the message
                Courier.shared.clickMessage(messageId = message.messageId) {
                    Courier.shared.client?.log("Clicked message: ${message.messageId}")
                }

                // Perform the callback
                onClickInboxMessageAtIndex?.invoke(message, index)

            }
            setOnLongPressMessageListener { message, index ->
                onLongPressInboxMessageAtIndex?.invoke(message, index)
            }
            setOnClickActionListener { action, message, index ->
                onClickInboxActionForMessageAtIndex?.invoke(action, message, index)
            }
            setOnScrollInboxListener { offsetInDp ->
                onScrollInbox?.invoke(offsetInDp)
            }
        }
    }

    internal val pages: List<Page> = listOf(
        Page(title = "Notifications", list = makeListView(InboxMessageFeed.FEED, context, attrs, defStyleAttr)),
        Page(title = "Archived", list = makeListView(InboxMessageFeed.ARCHIVE, context, attrs, defStyleAttr))
    )

    private val tabLayout: TabLayout by lazy { findViewById(R.id.tabLayout) }
    private val viewPager: ViewPager2 by lazy { findViewById(R.id.viewPager) }
    private val courierBar: CourierBar by lazy { findViewById(R.id.courierBar) }

    private var inboxListener: CourierInboxListener? = null

    init {
        View.inflate(context, R.layout.courier_inbox, this)
        setup()
        refreshTheme()
    }

    private fun setupViewPager() {

        viewPager.adapter = ViewPagerAdapter(pages)
        enableSwipe(canSwipePages)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            pages[position].tab = LayoutInflater.from(context).inflate(R.layout.courier_inbox_tab_item, null)
            tab.customView = pages[position].tab
        }.attach()

        theme.getTabLayoutIndicatorColor()?.let {
            tabLayout.setSelectedTabIndicatorColor(it)
        }

        refreshTabStyles()

    }

    private fun refreshTabStyles() {
        val selectedIndex = tabLayout.selectedTabPosition
        pages.forEachIndexed { index, page ->
            page.tab?.setStyles(index, isSelected = (index == selectedIndex))
        }
        tabLayout.getTabAt(selectedIndex)?.select()
    }

    private fun View.setStyles(index: Int, isSelected: Boolean) {
        val titleTextView = findViewById<TextView>(R.id.tab_title)
        titleTextView.text = pages[index].title
        titleTextView.setCourierFont(if (isSelected) theme.tabStyle.selected.font else theme.tabStyle.unselected.font)
        val titleBadgeView = findViewById<BadgeTextView>(R.id.tab_badge)
        titleBadgeView.setTheme(theme, isSelected)
    }

    private fun updateTabBadgeAt(index: Int, count: Int) {
        pages[index].tab?.let { tabView ->
            val titleBadgeView = tabView.findViewById<BadgeTextView>(R.id.tab_badge)
            titleBadgeView.text = "$count"
            titleBadgeView.isVisible = count > 0
        }
    }

    private fun refreshTheme() {
        theme = if (context.isDarkMode) darkTheme else lightTheme
    }

    private fun reloadViews() {
        courierBar.setBrand(theme.brand)
        pages.forEach { it.list.theme = theme }
        setupViewPager()
    }

    private fun setup() = coroutineScope.launch(Dispatchers.Main) {

        // Setup UI
        setupViewPager()

        // Add a TabSelectedListener to change the color when the tab is selected/unselected
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                pages[position].tab?.setStyles(position, isSelected = true)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                pages[position].tab?.setStyles(position, isSelected = false)
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                pages[position].tab?.setStyles(position, isSelected = true)
                tab?.position?.let {
                    val feed = if (it == 0) InboxMessageFeed.FEED else InboxMessageFeed.ARCHIVE
                    scrollToTop(feed)
                }
            }
        })

        // Grab the brand
        pages.forEach { it.list.setLoading(true) }
        refreshBrand()

        // Setup the listener
        inboxListener = Courier.shared.addInboxListener(
            onLoading = { isRefresh ->
                pages.forEach { it.list.setLoading(isRefresh) }
            },
            onError = { e ->
                pages.forEach { it.list.setError(e) }
            },
            onUnreadCountChanged = { count ->
                updateTabBadgeAt(index = 0, count)
            },
            onFeedChanged = { messageSet ->
                getPage(InboxMessageFeed.FEED).list.setMessageSet(messageSet)
            },
            onArchiveChanged = { messageSet ->
                getPage(InboxMessageFeed.ARCHIVE).list.setMessageSet(messageSet)
            },
            onPageAdded = { feed, messageSet ->
                getPage(feed).list.addPage(messageSet)
            },
            onMessageChanged = { feed, index, message ->
                getPage(feed).list.updateMessage(index, message)
            },
            onMessageAdded = { feed, index, message ->
                getPage(feed).list.addMessage(index, message)
            },
            onMessageRemoved = { feed, index, message ->
                getPage(feed).list.removeMessage(index, message)
            }
        )

    }

    internal suspend fun refreshBrand() {
        theme.getBrandIfNeeded()
        pages.forEach { it.list.theme = theme }
    }

    internal fun getPage(feed: InboxMessageFeed): Page {
        return if (feed == InboxMessageFeed.FEED) pages[0] else pages[1]
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        inboxListener?.remove()
    }

    private inner class ViewPagerAdapter(private val pages: List<Page>) : RecyclerView.Adapter<ViewPagerAdapter.PageViewHolder>() {

        inner class PageViewHolder(page: Page) : RecyclerView.ViewHolder(page.list) {
            init {
                page.list.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val page = pages[viewType]
            return PageViewHolder(page)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            // Empty
        }

        override fun getItemCount(): Int = pages.size

        override fun getItemViewType(position: Int) = position

    }

}

enum class InboxMessageFeed { FEED, ARCHIVE }

/**
 * Extensions
 */

fun CourierInbox.scrollToTop(feed: InboxMessageFeed) {
    getPage(feed).list.recyclerView.smoothScrollToPosition(0)
}