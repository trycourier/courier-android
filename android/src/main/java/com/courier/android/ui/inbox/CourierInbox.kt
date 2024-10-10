package com.courier.android.ui.inbox

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
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
import com.courier.android.modules.refreshInbox
import com.courier.android.utils.isDarkMode
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class CourierInbox @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    internal data class Page(val title: String, val list: InboxListView)

    internal enum class MessageFeed { FEED, ARCHIVE }

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

    private val pages = listOf(
        Page(title = "Notifications", list = InboxListView(context, attrs, defStyleAttr, MessageFeed.FEED)),
        Page(title = "Archived", list = InboxListView(context, attrs, defStyleAttr, MessageFeed.ARCHIVE))
    )

    private val tabLayout: TabLayout by lazy { findViewById(R.id.tabLayout) }
    private val viewPager: ViewPager2 by lazy { findViewById(R.id.viewPager) }

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

    private fun reloadViews() {

        print("Something")

    }

    private fun refreshTheme() {
        theme = if (context.isDarkMode) darkTheme else lightTheme
    }

    private fun setup() = coroutineScope.launch(Dispatchers.Main) {
        setupViewPager(viewPager)
        setupTabs()
        theme.getBrandIfNeeded()

//        // Setup the listener
//        inboxListener = Courier.shared.addInboxListener(
//            onInitialLoad = {
//
//                state = State.LOADING
//
//                refreshAdapters()
//
//                recyclerView.forceReactNativeLayoutFix()
//
//            },
//            onError = { e ->
//
//                state = State.ERROR.apply { title = e.message }
//
//                Courier.shared.client?.error(e.message)
//
//                refreshAdapters()
//
//                recyclerView.forceReactNativeLayoutFix()
//
//            },
//            onMessagesChanged = { messages, _, _, canPaginate ->
//
//                loadingAdapter.canPage = false
//
//                state = if (messages.isEmpty()) State.EMPTY.apply { title = "No messages found" } else State.CONTENT
//
//                refreshAdapters(
//                    showMessages = messages.isNotEmpty(),
//                    showLoading = canPaginate
//                )
//
//                refreshMessages(
//                    newMessages = messages.toList()
//                )
//
//                recyclerView.forceReactNativeLayoutFix()
//
//                loadingAdapter.canPage = canPaginate
//
//            }
//        )

    }

    internal fun refresh() = coroutineScope.launch(Dispatchers.Main) {
        theme.getBrandIfNeeded()
        Courier.shared.refreshInbox()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
//        inboxListener.remove()
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

    private inner class ViewPagerAdapter(private val pages: List<Page>) : RecyclerView.Adapter<ViewPagerAdapter.PageViewHolder>() {

        inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val container: FrameLayout = itemView.findViewById(R.id.inbox_container)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.inbox_page_layout, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val page = pages[position]
            holder.container.removeAllViews()
            holder.container.addView(page.list)
        }

        override fun getItemCount(): Int = pages.size
    }

}

/**
 * Extensions
 */

fun CourierInbox.scrollToTop() {
//    recyclerView.smoothScrollToPosition(0)
}