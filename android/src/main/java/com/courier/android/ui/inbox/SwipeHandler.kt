package com.courier.android.ui.inbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.Courier
import com.courier.android.R
import com.courier.android.modules.feedMessages
import kotlin.math.abs

internal class SwipeHandler(
    private val context: Context,
    private val onLeftToRightSwipe: (Int) -> Unit,
    private val onRightToLeftSwipe: (Int) -> Unit,
) {

    companion object {
        private const val READING_THRESHOLD = 0.25f
        private const val ARCHIVING_THRESHOLD = 0.5f
    }

    var readIcon: Int? = null
    var unreadIcon: Int? = null
    var archiveIcon: Int? = null
    var readBackgroundColor: Int? = null
    var unreadBackgroundColor: Int? = null
    var archiveBackgroundColor: Int? = null

    private fun getThemeColors(context: Context): Map<String, Int> {
        val primaryColor = getColorFromAttr(context, android.R.attr.colorPrimary)
        val greyColor = ContextCompat.getColor(context, android.R.color.darker_gray)
        val destructiveColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getColorFromAttr(context, android.R.attr.colorError)
        } else {
            ContextCompat.getColor(context, android.R.color.holo_red_light)
        }
        return mapOf(
            "primaryColor" to primaryColor,
            "greyColor" to greyColor,
            "destructiveColor" to destructiveColor
        )
    }

    // Helper function to get color from a theme attribute
    private fun getColorFromAttr(context: Context, attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private var currentSwipeDirection = 0
    private var pendingLeftToRightSwipeIndex: Int? = null

    private val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val index = viewHolder.layoutPosition
            when (direction) {
                ItemTouchHelper.LEFT -> onRightToLeftSwipe.invoke(index)
                ItemTouchHelper.RIGHT -> {
                    pendingLeftToRightSwipeIndex = index
                    (viewHolder.itemView.parent as? RecyclerView)?.adapter?.notifyItemChanged(index)
                }
            }
        }

        private fun getReadAssetForIndex(index: Int): Map<String, Int> {
            val isRead = Courier.shared.feedMessages[index].isRead
            return if (isRead) {
                mapOf(
                    "icon" to (unreadIcon ?: R.drawable.mark_email_unread),
                    "color" to (unreadBackgroundColor ?: getThemeColors(context)["greyColor"]!!)
                )
            } else {
                mapOf(
                    "icon" to (readIcon ?: R.drawable.mark_email_read),
                    "color" to (readBackgroundColor ?: getThemeColors(context)["primaryColor"]!!)
                )
            }
        }

        private var hasVibrated = false

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView

            if (currentSwipeDirection == 0 && dX != 0f) {
                currentSwipeDirection = if (dX > 0) 1 else -1
            }

            val didReachThreshold: Boolean

            val adjustedDx = if (dX > 0) {
                val threshold = itemView.width * READING_THRESHOLD
                didReachThreshold = dX >= threshold
                if (didReachThreshold) threshold + (dX - threshold) * READING_THRESHOLD else dX
            } else {
                val threshold = itemView.width * ARCHIVING_THRESHOLD
                didReachThreshold = abs(dX) >= threshold
                dX
            }

            // Reset the haptic vibration
            if (!didReachThreshold && hasVibrated) {
                hasVibrated = false
            }

            // Perform the haptic vibration here
            if (!hasVibrated && didReachThreshold) {
                hasVibrated = true
                itemView.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
            }

            val paint = Paint()

            if (adjustedDx > 0) {
                val position = viewHolder.layoutPosition
                val assets = getReadAssetForIndex(position)
                paint.color = assets["color"]!!
                c.drawRect(
                    itemView.left.toFloat(),
                    itemView.top.toFloat(),
                    itemView.left + adjustedDx,
                    itemView.bottom.toFloat(),
                    paint
                )
                val icon: Drawable? = ContextCompat.getDrawable(context, assets["icon"]!!)
                val iconMargin = (itemView.height - (icon?.intrinsicHeight ?: 0)) / 2
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + (icon?.intrinsicHeight ?: 0)
                val iconLeft = itemView.left + iconMargin
                val iconRight = iconLeft + (icon?.intrinsicWidth ?: 0)
                icon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon?.draw(c)
            } else if (adjustedDx < 0) {
                paint.color = archiveBackgroundColor ?: getThemeColors(context)["destructiveColor"]!!
                c.drawRect(
                    itemView.right + adjustedDx,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat(),
                    paint
                )
                val res = archiveIcon ?: R.drawable.archive
                val icon: Drawable? = ContextCompat.getDrawable(context, res)
                val iconMargin = (itemView.height - (icon?.intrinsicHeight ?: 0)) / 2
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + (icon?.intrinsicHeight ?: 0)
                val iconRight = itemView.right - iconMargin
                val iconLeft = iconRight - (icon?.intrinsicWidth ?: 0)
                icon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon?.draw(c)
            }

            if (dX == 0f) {
                hasVibrated = false
            }

            super.onChildDraw(c, recyclerView, viewHolder, adjustedDx, dY, actionState, isCurrentlyActive)
        }

        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
            return if (currentSwipeDirection == 1) READING_THRESHOLD else ARCHIVING_THRESHOLD
        }

        override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
            return if (currentSwipeDirection == 1) Float.MAX_VALUE else defaultValue
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            if (pendingLeftToRightSwipeIndex == viewHolder.layoutPosition) {
                onLeftToRightSwipe.invoke(viewHolder.layoutPosition)
                pendingLeftToRightSwipeIndex = null
            }
            currentSwipeDirection = 0
        }
    }

    private var swipeInteractionHelper: ItemTouchHelper? = null

    fun attach(recyclerView: RecyclerView) {
        swipeInteractionHelper = ItemTouchHelper(itemTouchHelper)
        swipeInteractionHelper?.attachToRecyclerView(recyclerView)
    }

    fun remove() {
        swipeInteractionHelper?.attachToRecyclerView(null)
        swipeInteractionHelper = null
    }

}
