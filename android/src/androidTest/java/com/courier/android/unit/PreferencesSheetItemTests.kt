package com.courier.android.unit

import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.SwitchCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.R
import com.courier.android.ui.preferences.CourierPreferencesTheme
import com.courier.android.ui.preferences.CourierSheetItem
import com.courier.android.ui.preferences.PreferencesSheetItemAdapter
import com.courier.android.ui.preferences.SheetItemViewHolder
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// The topic sheet saves the items it was told about, so every way of flipping a row's switch has
// to report the change. A tap on the switch itself used to flip it without reporting anything.
@RunWith(AndroidJUnit4::class)
class PreferencesSheetItemTests {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun item(isOn: Boolean) = CourierSheetItem(title = "Inbox", isOn = isOn, isDisabled = false, data = null)

    private fun bindRow(items: List<CourierSheetItem>, changes: MutableList<CourierSheetItem>): SheetItemViewHolder {
        val context = ContextThemeWrapper(instrumentation.targetContext, androidx.appcompat.R.style.Theme_AppCompat_Light)
        val adapter = PreferencesSheetItemAdapter(CourierPreferencesTheme(), items) { changed, _ -> changes.add(changed) }
        val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
        adapter.onBindViewHolder(holder, 0)
        return holder
    }

    @Test
    fun tappingTheSwitchReportsTheChange() {
        val changes = mutableListOf<CourierSheetItem>()
        instrumentation.runOnMainSync {
            val holder = bindRow(listOf(item(isOn = false)), changes)
            holder.itemView.findViewById<SwitchCompat>(R.id.switchView).performClick()
        }
        assertEquals(listOf(true), changes.map { it.isOn })
    }

    @Test
    fun tappingTheRowReportsTheChange() {
        val changes = mutableListOf<CourierSheetItem>()
        instrumentation.runOnMainSync {
            val holder = bindRow(listOf(item(isOn = true)), changes)
            holder.itemView.findViewById<LinearLayoutCompat>(R.id.container).performClick()
        }
        assertEquals(listOf(false), changes.map { it.isOn })
    }

    @Test
    fun bindingARecycledRowReportsNothing() {
        val changes = mutableListOf<CourierSheetItem>()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, androidx.appcompat.R.style.Theme_AppCompat_Light)
            val adapter = PreferencesSheetItemAdapter(CourierPreferencesTheme(), listOf(item(isOn = false), item(isOn = true))) { changed, _ -> changes.add(changed) }
            val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
            adapter.onBindViewHolder(holder, 0)
            adapter.onBindViewHolder(holder, 1)
        }
        assertEquals(emptyList<CourierSheetItem>(), changes)
    }

}
