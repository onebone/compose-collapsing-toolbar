/*
 * Copyright (c) 2021 onebone <me@onebone.me>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

package me.onebone.toolbar

import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker

/**
 * HACK: Compose tracks velocity with a local coordinate system which leads to an undesired
 * scroll experience. To mitigate this issue, we use RelativeVelocityTracker which tracks velocity
 * with a global coordinate system. In NestedScrollConnection, onPreScroll() gives us a delta
 * based on a global coordinate, we can use this value to properly calculate the velocity.
 *
 * The fundamental goal of this class is to override the Compose-calculated scroll velocity to
 * our manually calculated one.
 *
 * @see <a href="https://github.com/onebone/compose-collapsing-toolbar/issues/7">this issue</a>
 */
internal class RelativeVelocityTracker(
	private val timeProvider: CurrentTimeProvider
) {
	private val tracker = VelocityTracker()
	private var lastY: Float? = null

	fun delta(delta: Float) {
		val new = (lastY ?: 0f) + delta

		tracker.addPosition(timeProvider.now(), Offset(0f, new))
		lastY = new
	}

	fun reset(): Float {
		lastY = null

		val velocity = tracker.calculateVelocity()
		tracker.resetTracking()

		return velocity.y
	}
}

/**
 * [androidx.compose.ui.input.nestedscroll.NestedScrollConnection.onPreFling] subtracts its scroll
 * value by the returned value to calculate a remaining velocity.
 *
 * This function provides a delta that will override the remaining value that is calculated by the
 * compose framework.
 *
 * @see <a href="https://github.com/onebone/compose-collapsing-toolbar/issues/7">this issue</a>
 */
internal fun RelativeVelocityTracker.deriveDelta(initial: Float) =
	initial - reset()

internal interface CurrentTimeProvider {
	fun now(): Long
}

internal class CurrentTimeProviderImpl: CurrentTimeProvider {
	override fun now(): Long =
		SystemClock.uptimeMillis()
}
