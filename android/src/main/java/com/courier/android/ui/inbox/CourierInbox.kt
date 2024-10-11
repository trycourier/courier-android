package com.courier.android.ui.inbox

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.R
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.InboxAction
import com.courier.android.models.InboxMessage
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.refreshInbox
import com.courier.android.ui.bar.CourierBar
import com.courier.android.utils.isDarkMode
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class CourierInbox @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    internal data class Page(val title: String, val list: InboxListView)

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

    private fun makeListView(feed: InboxMessageFeed, context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): InboxListView {
        return InboxListView(context, attrs, defStyleAttr, feed).apply {
            setOnClickMessageListener { message, index ->
                onClickInboxMessageAtIndex?.invoke(message, index)
            }
            setOnClickActionListener { action, message, index ->
                onClickInboxActionForMessageAtIndex?.invoke(action, message, index)
            }
            setOnScrollInboxListener { offsetInDp ->
                onScrollInbox?.invoke(offsetInDp)
            }
        }
    }

    private val pages: List<Page> = listOf(
        Page(title = "Notifications", list = makeListView(InboxMessageFeed.FEED, context, attrs, defStyleAttr)),
        Page(title = "Archived", list = makeListView(InboxMessageFeed.ARCHIVE, context, attrs, defStyleAttr))
    )

    private val tabLayout: TabLayout by lazy { findViewById(R.id.tabLayout) }
    private val viewPager: ViewPager2 by lazy { findViewById(R.id.viewPager) }
    private val courierBar: CourierBar by lazy { findViewById(R.id.courierBar) }

    private lateinit var inboxListener: CourierInboxListener

    private var onClickInboxMessageAtIndex: ((InboxMessage, Int) -> Unit)? = null
    private var onClickInboxActionForMessageAtIndex: ((InboxAction, InboxMessage, Int) -> Unit)? = null
    private var onScrollInbox: ((Int) -> Unit)? = null

    init {
        View.inflate(context, R.layout.courier_inbox, this)
        refreshTheme()
        setup()
    }

    private fun setupViewPager(viewPager: ViewPager2) {
        viewPager.adapter = ViewPagerAdapter(pages)
    }

    private fun setupTabs() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pages[position].title
        }.attach()
    }

    private fun refreshTheme() {
        theme = if (context.isDarkMode) darkTheme else lightTheme
    }

    private fun reloadViews() {
        courierBar.setBrand(theme.brand)
    }

    private fun setup() = coroutineScope.launch(Dispatchers.Main) {

        // Setup UI
        setupViewPager(viewPager)
        setupTabs()

        // Grab the brand
        pages.forEach { it.list.setLoading(false) }
        theme.getBrandIfNeeded()

        // Setup the listener
        inboxListener = Courier.shared.addInboxListener(
            onLoading = { isRefresh ->
                pages.forEach { it.list.setLoading(isRefresh) }
            },
            onError = { e ->
                pages.forEach { it.list.setError(e) }
            },
            onUnreadCountChanged = { count ->
                println("Unread count: $count")  // Example print for unread count
            },
            onFeedChanged = { messageSet ->
                pages[0].list.setMessageSet(messageSet)
            },
            onArchiveChanged = { messageSet ->
                pages[1].list.setMessageSet(messageSet)
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
                getPage(feed).list.removeMessage(index, message) // Example print for message removed
            }
        )

    }

    private fun getPage(feed: InboxMessageFeed): Page {
        return if (feed == InboxMessageFeed.FEED) pages[0] else pages[1]
    }

    fun setOnClickMessageListener(listener: ((message: InboxMessage, index: Int) -> Unit)?) {
        onClickInboxMessageAtIndex = listener
    }

    fun setOnClickActionListener(listener: ((action: InboxAction, message: InboxMessage, index: Int) -> Unit)?) {
        onClickInboxActionForMessageAtIndex = listener
    }

    fun setOnScrollInboxListener(listener: ((offsetInDp: Int) -> Unit)?) {
        onScrollInbox = listener
    }

    internal fun refresh() = coroutineScope.launch(Dispatchers.Main) {
        theme.getBrandIfNeeded()
        Courier.shared.refreshInbox()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        inboxListener.remove()
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
            val page = pages[position]
        }

        override fun getItemCount(): Int = pages.size

        override fun getItemViewType(position: Int) = position

    }

}

enum class InboxMessageFeed { FEED, ARCHIVE }

/**
 * Extensions
 */

fun CourierInbox.scrollToTop() {
//    recyclerView.smoothScrollToPosition(0)
}