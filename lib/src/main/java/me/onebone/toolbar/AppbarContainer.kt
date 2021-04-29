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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun AppbarContainer(
	modifier: Modifier = Modifier,
	/** The state of a connected collapsing toolbar */
	collapsingToolbarState: CollapsingToolbarState,
	content: @Composable AppbarContainerScope.() -> Unit
) {
	val offsetY = remember { mutableStateOf(0) }
	val toolbarHeight = remember { mutableStateOf(0) }

	val nestedScrollConnection = remember {
		AppbarNestedScrollConnection(toolbarHeight, offsetY)
	}

	collapsingToolbarState.onVisibleHeightChange = CollapsingToolbarVisibleHeightChangeListener {
		toolbarHeight.value = it
	}

	val scope = remember { AppbarContainerScopeImpl(nestedScrollConnection) }
	val measurePolicy = remember { AppbarMeasurePolicy(offsetY) }

	Layout(
		content = { scope.content() },
		measurePolicy = measurePolicy,
		modifier = modifier
	)
}

interface AppbarContainerScope {
	fun Modifier.appBarBody(): Modifier
}

private class AppbarNestedScrollConnection(
	private val toolbarHeight: State<Int>,
	private val offsetY: MutableState<Int>
): NestedScrollConnection {
	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		val dy = available.y

		val toolbar = toolbarHeight.value.toFloat()
		val offset = offsetY.value.toFloat()

		// -toolbarHeight <= offsetY + dy <= 0
		val consume = if(dy < 0) { // scrolling down
			// -toolbarHeight - offset <= dy
			dy.coerceAtLeast(-toolbar - offset)
		}else{
			// dy <= -offsetY
			dy.coerceAtMost(-offset)
		}

		offsetY.value += consume.roundToInt()

		return Offset(0f, consume)
	}
}

private class AppbarContainerScopeImpl(
	private val nestedScrollConnection: NestedScrollConnection
): AppbarContainerScope {
	override fun Modifier.appBarBody(): Modifier {
		return this.then(AppBarBodyMarkerModifier).nestedScroll(nestedScrollConnection)
	}
}

private object AppBarBodyMarkerModifier: ParentDataModifier {
	override fun Density.modifyParentData(parentData: Any?): Any {
		return AppBarBodyMarker
	}
}

private object AppBarBodyMarker

private class AppbarMeasurePolicy(
	private val offsetY: State<Int>
): MeasurePolicy {
	override fun MeasureScope.measure(
		measurables: List<Measurable>,
		constraints: Constraints
	): MeasureResult {
		if(measurables.size > 2)
			throw IllegalStateException("AppbarContainer could hold at most 2 compose nodes")

		var width = 0
		var height = 0

		var toolbarPlaceable: Placeable? = null

		val placeables = measurables.mapIndexed { i, measurable ->
			val placeable = measurable.measure(constraints)

			width = max(width, placeable.width)
			height = max(height, placeable.height)

			val data = measurable.parentData
			if(data != AppBarBodyMarker) {
				toolbarPlaceable = placeable
			}

			placeable
		}

		return layout(
			width.coerceIn(constraints.minWidth, constraints.maxWidth),
			height.coerceIn(constraints.minHeight, constraints.maxHeight)
		) {
			placeables.forEach { placeable ->
				placeable.place(
					x = 0,
					y = offsetY.value +
							if(toolbarPlaceable == placeable) 0
							else toolbarPlaceable?.height ?: 0
				)
			}
		}
	}
}
