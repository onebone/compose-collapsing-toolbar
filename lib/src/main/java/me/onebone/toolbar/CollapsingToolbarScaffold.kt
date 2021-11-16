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

import android.os.Bundle
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import kotlin.math.max

@Stable
class CollapsingToolbarScaffoldState(
	val toolbarState: CollapsingToolbarState,
	initialOffsetY: Int = 0
) {
	val offsetY: Int
		get() = offsetYState.value

	internal val offsetYState = mutableStateOf(initialOffsetY)
}

private class CollapsingToolbarScaffoldStateSaver: Saver<CollapsingToolbarScaffoldState, Bundle> {
	override fun restore(value: Bundle): CollapsingToolbarScaffoldState =
		CollapsingToolbarScaffoldState(
			CollapsingToolbarState(value.getInt("height", Int.MAX_VALUE)),
			value.getInt("offsetY", 0)
		)

	override fun SaverScope.save(value: CollapsingToolbarScaffoldState): Bundle =
		Bundle().apply {
			putInt("height", value.toolbarState.height)
			putInt("offsetY", value.offsetY)
		}
}

@Composable
fun rememberCollapsingToolbarScaffoldState(
	toolbarState: CollapsingToolbarState = rememberCollapsingToolbarState()
): CollapsingToolbarScaffoldState {
	return rememberSaveable(toolbarState, saver = CollapsingToolbarScaffoldStateSaver()) {
		CollapsingToolbarScaffoldState(toolbarState)
	}
}

@Composable
fun CollapsingToolbarScaffold(
	modifier: Modifier,
	state: CollapsingToolbarScaffoldState,
	scrollStrategy: ScrollStrategy,
	enabled: Boolean = true,
	toolbarModifier: Modifier = Modifier,
	toolbar: @Composable CollapsingToolbarScope.() -> Unit,
	body: @Composable () -> Unit
) {
	val flingBehavior = ScrollableDefaults.flingBehavior()

	val nestedScrollConnection = remember(scrollStrategy, state) {
		scrollStrategy.create(state.offsetYState, state.toolbarState, flingBehavior)
	}

	val toolbarState = state.toolbarState

	SubcomposeLayout(
		modifier = modifier
			.then(
				if(enabled) {
					Modifier.nestedScroll(nestedScrollConnection)
				}else{
					Modifier
				}
			)
	) { constraints ->
		val toolbarConstraints = constraints.copy(
			minWidth = 0,
			minHeight = 0
		)

		val toolbarPlaceables = subcompose(CollapsingToolbarScaffoldContent.Toolbar) {
			CollapsingToolbar(
				modifier = toolbarModifier,
				collapsingToolbarState = toolbarState
			) {
				toolbar()
			}
		}.map { it.measure(toolbarConstraints) }

		val bodyConstraints = constraints.copy(
			minWidth = 0,
			minHeight = 0,
			maxHeight = when(scrollStrategy) {
				ScrollStrategy.ExitUntilCollapsed ->
					(constraints.maxHeight - toolbarState.minHeight).coerceAtLeast(0)

				ScrollStrategy.EnterAlways, ScrollStrategy.EnterAlwaysCollapsed ->
					constraints.maxHeight
			}
		)

		val bodyPlaceables = subcompose(CollapsingToolbarScaffoldContent.Body) {
			body()
		}.map { it.measure(bodyConstraints) }

		val width = max(
			toolbarPlaceables.maxOfOrNull { it.width } ?: 0,
			bodyPlaceables.maxOfOrNull { it.width } ?: 0
		).coerceIn(constraints.minWidth, constraints.maxWidth)

		val toolbarHeight = toolbarPlaceables.maxOfOrNull { it.height } ?: 0

		val height = max(
			toolbarHeight,
			bodyPlaceables.maxOfOrNull { it.height } ?: 0
		).coerceIn(constraints.minHeight, constraints.maxHeight)

		layout(width, height) {
			bodyPlaceables.forEach {
				it.place(0, toolbarHeight + state.offsetY)
			}

			toolbarPlaceables.forEach {
				it.place(0, state.offsetY)
			}
		}
	}
}

private enum class CollapsingToolbarScaffoldContent {
	Toolbar, Body
}
