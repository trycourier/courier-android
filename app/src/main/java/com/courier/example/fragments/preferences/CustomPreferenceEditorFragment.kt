package com.courier.example.fragments.preferences

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.courier.example.R
import com.courier.example.databinding.ActivityMainBinding

class CustomPreferenceEditorFragment : Fragment(R.layout.fragment_custom_preference_editor) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater).apply {
//            setContentView(root)
//        }
//
//        setCurrentFragment(authFragment)
//
//        binding.bottomNavigationView.setOnItemSelectedListener {
//            return@setOnItemSelectedListener when (it.itemId) {
//                R.id.auth -> setCurrentFragment(authFragment)
//                R.id.inbox -> setCurrentFragment(inboxFragment)
//                R.id.send -> setCurrentFragment(sendFragment)
//                R.id.preferences -> setCurrentFragment(preferenceFragment)
//                else -> false
//            }
//        }

}