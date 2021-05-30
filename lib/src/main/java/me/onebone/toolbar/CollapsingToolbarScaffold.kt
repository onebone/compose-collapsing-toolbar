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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import kotlin.math.max

@Composable
fun CollapsingToolbarScaffold(
	modifier: Modifier,
	state: CollapsingToolbarState,
	scrollStrategy: ScrollStrategy,
	toolbar: @Composable CollapsingToolbarScope.() -> Unit,
	body: @Composable () -> Unit
) {
	val offsetY = rememberSaveable { mutableStateOf(0) }

	val nestedScrollConnection = remember(scrollStrategy) {
		scrollStrategy.create(offsetY, state)
	}

	SubcomposeLayout(
		modifier = modifier
			.nestedScroll(nestedScrollConnection)
	) { constraints ->
		val toolbarConstraints = constraints.copy(
			minWidth = 0,
			minHeight = 0
		)

		val bodyConstraints = constraints.copy(
			minWidth = 0,
			minHeight = 0,
			maxHeight =
				if(scrollStrategy == ScrollStrategy.ExitUntilCollapsed)
					max(0, constraints.maxHeight - state.minHeight)
				else
					constraints.maxHeight
		)

		val toolbarPlaceables = subcompose(CollapsingToolbarScaffoldContent.Toolbar) {
			CollapsingToolbar(collapsingToolbarState = state) {
				toolbar()
			}
		}.map { it.measure(toolbarConstraints) }

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
				it.place(0, toolbarHeight + offsetY.value)
			}

			toolbarPlaceables.forEach {
				it.place(0, offsetY.value)
			}
		}
	}
}

private enum class CollapsingToolbarScaffoldContent {
	Toolbar, Body
}
