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

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.absoluteValue

enum class ScrollStrategy {
	EnterAlways {
		override fun create(
			scaffoldState: CollapsingToolbarScaffoldState,
			flingBehavior: FlingBehavior,
			snapConfig: SnapConfig?,
		): NestedScrollConnection =
			EnterAlwaysNestedScrollConnection(scaffoldState, flingBehavior, snapConfig)
	},
	EnterAlwaysCollapsed {
		override fun create(
			scaffoldState: CollapsingToolbarScaffoldState,
			flingBehavior: FlingBehavior,
			snapConfig: SnapConfig?,
		): NestedScrollConnection =
			EnterAlwaysCollapsedNestedScrollConnection(scaffoldState, flingBehavior, snapConfig)
	},
	ExitUntilCollapsed {
		override fun create(
			scaffoldState: CollapsingToolbarScaffoldState,
			flingBehavior: FlingBehavior,
			snapConfig: SnapConfig?,
		): NestedScrollConnection =
			ExitUntilCollapsedNestedScrollConnection(scaffoldState, flingBehavior, snapConfig)
	};

	internal abstract fun create(
		scaffoldState: CollapsingToolbarScaffoldState,
		flingBehavior: FlingBehavior,
		snapConfig: SnapConfig?,
	): NestedScrollConnection
}

private class ScrollDelegate(
	private val offsetY: MutableState<Int>
) {
	private var scrollToBeConsumed: Float = 0f

	fun doScroll(delta: Float) {
		val scroll = scrollToBeConsumed + delta
		val scrollInt = scroll.toInt()

		scrollToBeConsumed = scroll - scrollInt

		offsetY.value += scrollInt
	}
}

internal class EnterAlwaysNestedScrollConnection(
	private val scaffoldState: CollapsingToolbarScaffoldState,
	private val flingBehavior: FlingBehavior,
	private val snapConfig: SnapConfig?
): NestedScrollConnection {
	private val scrollDelegate = ScrollDelegate(scaffoldState.offsetYState)
	private val tracker = RelativeVelocityTracker(CurrentTimeProviderImpl())
	private val toolbarState = scaffoldState.toolbarState

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		val dy = available.y
		tracker.delta(dy)

		val toolbar = toolbarState.height.toFloat()
		val offset = scaffoldState.offsetY.toFloat()

		// -toolbarHeight <= offsetY + dy <= 0
		val consume = if(dy < 0) {
			val toolbarConsumption = toolbarState.dispatchRawDelta(dy)
			val remaining = dy - toolbarConsumption
			val offsetConsumption = remaining.coerceAtLeast(-toolbar - offset)
			scrollDelegate.doScroll(offsetConsumption)

			toolbarConsumption + offsetConsumption
		}else{
			val offsetConsumption = dy.coerceAtMost(-offset)
			scrollDelegate.doScroll(offsetConsumption)

			val toolbarConsumption = toolbarState.dispatchRawDelta(dy - offsetConsumption)

			offsetConsumption + toolbarConsumption
		}

		return Offset(0f, consume)
	}

	override suspend fun onPreFling(available: Velocity): Velocity {
		val velocity = tracker.reset()

		val left = if(velocity > 0) {
			toolbarState.fling(flingBehavior, velocity)
		}else{
			// If velocity < 0, the main content should have a remaining scroll space
			// so the scroll resumes to the onPreScroll(..., Fling) phase. Hence we do
			// not need to process it at onPostFling() manually.
			velocity
		}

		return available.copy(y = available.y - left)
	}

	override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
		// TODO: Cancel expand/collapse animation inside onPreScroll
		snapConfig?.let {
			val isToolbarChangingOffset = scaffoldState.offsetY != 0
			if (isToolbarChangingOffset) {
				// When the toolbar is hiding, it does it through changing the offset and does not
				// change its height, so we must process not the snap of the toolbar, but the
				// snap of its offset.
				scaffoldState.performOffsetSnap(it)
			} else {
				toolbarState.performSnap(it)
			}
		}

		return super.onPostFling(consumed, available)
	}
}

internal class EnterAlwaysCollapsedNestedScrollConnection(
	private val scaffoldState: CollapsingToolbarScaffoldState,
	private val flingBehavior: FlingBehavior,
	private val snapConfig: SnapConfig?,
): NestedScrollConnection {
	private val scrollDelegate = ScrollDelegate(scaffoldState.offsetYState)
	private val tracker = RelativeVelocityTracker(CurrentTimeProviderImpl())
	private val toolbarState = scaffoldState.toolbarState

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		val dy = available.y
		tracker.delta(dy)

		val consumed = if(dy > 0) { // expanding: offset -> body -> toolbar
			val offsetConsumption = dy.coerceAtMost(-scaffoldState.offsetY.toFloat())
			scrollDelegate.doScroll(offsetConsumption)

			offsetConsumption
		}else{ // collapsing: toolbar -> offset -> body
			val toolbarConsumption = toolbarState.dispatchRawDelta(dy)
			val offsetConsumption = (dy - toolbarConsumption)
				.coerceAtLeast(-toolbarState.height.toFloat() - scaffoldState.offsetY)

			scrollDelegate.doScroll(offsetConsumption)

			toolbarConsumption + offsetConsumption
		}

		return Offset(0f, consumed)
	}

	override fun onPostScroll(
		consumed: Offset,
		available: Offset,
		source: NestedScrollSource
	): Offset {
		val dy = available.y

		return if(dy > 0) {
			Offset(0f, toolbarState.dispatchRawDelta(dy))
		}else{
			Offset(0f, 0f)
		}
	}

	override suspend fun onPreFling(available: Velocity): Velocity =
		available.copy(y = tracker.deriveDelta(available.y))

	override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
		val dy = available.y

		val left = if(dy > 0) {
			// onPostFling() has positive available scroll value only called if the main scroll
			// has leftover scroll, i.e. the scroll of the main content has done. So we just process
			// fling if the available value is positive.
			toolbarState.fling(flingBehavior, dy)
		}else{
			dy
		}

		// TODO: Cancel expand/collapse animation inside onPreScroll
		snapConfig?.let {
			val isToolbarChangingOffset = scaffoldState.offsetY != 0
			if (isToolbarChangingOffset) {
				// When the toolbar is hiding, it does it through changing the offset and does not
				// change its height, so we must process not the snap of the toolbar, but the
				// snap of its offset.
				scaffoldState.performOffsetSnap(it)
			} else {
				toolbarState.performSnap(it)
			}
		}

		return available.copy(y = available.y - left)
	}
}

internal class ExitUntilCollapsedNestedScrollConnection(
	private val scaffoldState: CollapsingToolbarScaffoldState,
	private val flingBehavior: FlingBehavior,
	private val snapConfig: SnapConfig?
): NestedScrollConnection {
	private val tracker = RelativeVelocityTracker(CurrentTimeProviderImpl())
	private val toolbarState = scaffoldState.toolbarState

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		val dy = available.y
		tracker.delta(dy)

		val consume = if(dy < 0) { // collapsing: toolbar -> body
			toolbarState.dispatchRawDelta(dy)
		}else{
			0f
		}

		return Offset(0f, consume)
	}

	override fun onPostScroll(
		consumed: Offset,
		available: Offset,
		source: NestedScrollSource
	): Offset {
		val dy = available.y

		val consume = if(dy > 0) { // expanding: body -> toolbar
			toolbarState.dispatchRawDelta(dy)
		}else{
			0f
		}

		return Offset(0f, consume)
	}

	override suspend fun onPreFling(available: Velocity): Velocity {
		val velocity = tracker.reset()

		val left = if(velocity < 0) {
			toolbarState.fling(flingBehavior, velocity)
		}else{
			velocity
		}

		return available.copy(y = available.y - left)
	}

	override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
		val velocity = available.y

		val left = if(velocity > 0) {
			toolbarState.fling(flingBehavior, velocity)
		}else{
			velocity
		}

		// TODO: Cancel expand/collapse animation inside onPreScroll
		snapConfig?.let { scaffoldState.toolbarState.performSnap(it) }

		return available.copy(y = available.y - left)
	}
}

// TODO: Is there a better solution rather OptIn ExperimentalToolbarApi?
@OptIn(ExperimentalToolbarApi::class)
private suspend fun CollapsingToolbarState.performSnap(snapConfig: SnapConfig) {
	if (progress > snapConfig.edge) {
		expand(snapConfig.expandDuration)
	} else {
		collapse(snapConfig.collapseDuration)
	}
}

// TODO: Is there a better solution rather OptIn ExperimentalToolbarApi?
@OptIn(ExperimentalToolbarApi::class)
private suspend fun CollapsingToolbarScaffoldState.performOffsetSnap(snapConfig: SnapConfig) {
	if (toolbarState.minHeight == 0) return

	val offsetProgress = 1f - (offsetY / toolbarState.minHeight).absoluteValue
	if (offsetProgress > snapConfig.edge) {
		expandOffset(snapConfig.expandDuration)
	} else {
		collapseOffset(snapConfig.collapseDuration)
	}
}
