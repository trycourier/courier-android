package com.courier.example.fragments

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.courier.android.Courier
import com.courier.android.modules.readAllInboxMessages
import com.courier.example.R
import com.courier.example.fragments.inbox.CustomInboxFragment
import com.courier.example.fragments.inbox.PrebuiltInboxFragment
import com.courier.example.fragments.inbox.StyledInboxFragment
import com.google.android.material.tabs.TabLayout

class InboxFragment: Fragment(R.layout.fragment_inbox) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the menu
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setOnMenuItemClickListener { _ ->
            Courier.shared.readAllInboxMessages(
                onSuccess = {
                    Toast.makeText(context, "All Messages Read", Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    print(error)
                }
            )
            return@setOnMenuItemClickListener true
        }

        val viewPager = view.findViewById<ViewPager>(R.id.viewPager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)

        val adapter = ViewPagerAdapter(childFragmentManager)
        adapter.addFragment(PrebuiltInboxFragment(), "Default")
        adapter.addFragment(StyledInboxFragment(), "Styled")
        adapter.addFragment(CustomInboxFragment(), "Custom")

        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

    }

    private inner class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private val fragmentList = ArrayList<Fragment>()
        private val titleList = ArrayList<String>()

        override fun getItem(position: Int): Fragment = fragmentList[position]

        override fun getCount(): Int = fragmentList.size

        override fun getPageTitle(position: Int): CharSequence = titleList[position]

        fun addFragment(fragment: Fragment, title: String) {
            fragmentList.add(fragment)
            titleList.add(title)
        }

    }

}

class NoSwipeViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {

    // Flag to enable or disable swiping
    var isPagingEnabled: Boolean = false

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        // Disable swipe if paging is disabled
        return isPagingEnabled && super.onTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // Disable swipe if paging is disabled
        return isPagingEnabled && super.onInterceptTouchEvent(ev)
    }
}