package com.courier.android.ui.inbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

internal class SwipeHandler(
    private val context: Context,
    private val rightToLeftSwipeBackgroundColor: Int,
    private val leftToRightSwipeBackgroundColor: Int,
    private val rightToLeftSwipeIconResId: Int,
    private val leftToRightSwipeIconResId: Int,
    private val onLeftToRightSwipe: (Int) -> Unit,
    private val onRightToLeftSwipe: (Int) -> Unit,
) {

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
                // Set the background color for right swipe
                paint.color = leftToRightSwipeBackgroundColor

                // Draw the background
                c.drawRect(
                    itemView.left.toFloat(),
                    itemView.top.toFloat(),
                    dX,
                    itemView.bottom.toFloat(),
                    paint
                )

                // Set the icon for right swipe
                val icon: Drawable? = ContextCompat.getDrawable(context, leftToRightSwipeIconResId)
                val iconMargin = (itemView.height - (icon?.intrinsicHeight ?: 0)) / 2
                val iconTop = itemView.top + (itemView.height - (icon?.intrinsicHeight ?: 0)) / 2
                val iconBottom = iconTop + (icon?.intrinsicHeight ?: 0)
                val iconLeft = itemView.left + iconMargin
                val iconRight = iconLeft + (icon?.intrinsicWidth ?: 0)

                // Set the bounds and draw the icon
                icon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon?.draw(c)

            } else if (dX < 0) { // Swiping to the left (e.g., delete)
                // Set the background color for left swipe
                paint.color = rightToLeftSwipeBackgroundColor

                // Draw the background
                c.drawRect(
                    itemView.right + dX,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat(),
                    paint
                )

                // Set the icon for left swipe
                val icon: Drawable? = ContextCompat.getDrawable(context, rightToLeftSwipeIconResId)
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

    fun attach(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(itemTouchHelper)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

}
