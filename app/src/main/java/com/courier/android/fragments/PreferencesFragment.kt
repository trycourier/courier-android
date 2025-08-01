package com.courier.android.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.courier.android.R
import com.courier.android.fragments.preferences.CustomPreferencesFragment
import com.courier.android.fragments.preferences.PrebuiltPreferencesFragment
import com.courier.android.fragments.preferences.StyledPreferencesFragment
import com.google.android.material.tabs.TabLayout

class PreferencesFragment: Fragment(R.layout.fragment_preferences) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById<ViewPager>(R.id.viewPager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)

        val adapter = ViewPagerAdapter(childFragmentManager)
        adapter.addFragment(PrebuiltPreferencesFragment(), "Default")
        adapter.addFragment(StyledPreferencesFragment(), "Styled")
        adapter.addFragment(CustomPreferencesFragment(), "Custom")

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