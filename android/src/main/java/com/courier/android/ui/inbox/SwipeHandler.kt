package com.courier.android.ui.inbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.Courier
import com.courier.android.R
import com.courier.android.modules.inboxData

internal class SwipeHandler(
    private val context: Context,
    private val onLeftToRightSwipe: (Int) -> Unit,
    private val onRightToLeftSwipe: (Int) -> Unit,
) {

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

    private val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            // We are not supporting drag & drop, so return false
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            // Get the index
            val index = viewHolder.layoutPosition

            // Hit the callback
            when (direction) {
                ItemTouchHelper.LEFT -> {
                    onRightToLeftSwipe.invoke(index)
                }
                ItemTouchHelper.RIGHT -> {
                    onLeftToRightSwipe.invoke(index)
                }
            }
        }

        private fun getReadAssetForIndex(index: Int): Map<String, Int> {
            val isRead = Courier.shared.inboxData?.feed?.messages?.get(index)?.isRead
            return if (isRead == true) {
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

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            // Draw background and icon during the swipe
            val itemView = viewHolder.itemView
            val paint = Paint()

            if (dX > 0) {

                val position = viewHolder.layoutPosition
                val assets = getReadAssetForIndex(position)

                // Set the background color for right swipe
                paint.color = assets["color"]!!

                // Draw the background
                c.drawRect(
                    itemView.left.toFloat(),
                    itemView.top.toFloat(),
                    dX,
                    itemView.bottom.toFloat(),
                    paint
                )

                // Set the icon for right swipe
                val icon: Drawable? = ContextCompat.getDrawable(context, assets["icon"]!!)
                val iconMargin = (itemView.height - (icon?.intrinsicHeight ?: 0)) / 2
                val iconTop = itemView.top + (itemView.height - (icon?.intrinsicHeight ?: 0)) / 2
                val iconBottom = iconTop + (icon?.intrinsicHeight ?: 0)
                val iconLeft = itemView.left + iconMargin
                val iconRight = iconLeft + (icon?.intrinsicWidth ?: 0)

                // Set the bounds and draw the icon
                icon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon?.draw(c)

            } else if (dX < 0) {

                // Set the background color for left swipe
                paint.color = archiveBackgroundColor ?: getThemeColors(context)["destructiveColor"]!!

                // Draw the background
                c.drawRect(
                    itemView.right + dX,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat(),
                    paint
                )

                // Set the icon for left swipe
                val res = archiveIcon ?: R.drawable.archive
                val icon: Drawable? = ContextCompat.getDrawable(context, res)
                val iconMargin = (itemView.height - (icon?.intrinsicHeight ?: 0)) / 2
                val iconTop = itemView.top + (itemView.height - (icon?.intrinsicHeight ?: 0)) / 2
                val iconBottom = iconTop + (icon?.intrinsicHeight ?: 0)
                val iconRight = itemView.right - iconMargin
                val iconLeft = iconRight - (icon?.intrinsicWidth ?: 0)

                // Set the bounds and draw the icon
                icon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon?.draw(c)
            }

            // Call the super method last to draw the item on top of the background and icon
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
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
