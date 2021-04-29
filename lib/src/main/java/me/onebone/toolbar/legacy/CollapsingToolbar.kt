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

package me.onebone.toolbar.legacy

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CollapsingToolbar(
		modifier: Modifier = Modifier,
		content: @Composable CollapsingToolbarScope.() -> Unit
) {
	val offsetY = remember { mutableStateOf(0) }
	val toolbarHeight = remember { mutableStateOf(0) }

	val nestedScrollConnection = object: NestedScrollConnection {
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

	val scope = remember { CollapsingToolbarScopeImpl(nestedScrollConnection) }

	Layout(
		content = { scope.content() },
		modifier = modifier,
		measurePolicy = ToolbarMeasurePolicy(toolbarHeight, offsetY)
	)
}

internal class ToolbarMeasurePolicy(
	private val toolbarHeight: MutableState<Int>,
	private val offsetY: State<Int>
): MeasurePolicy {
	override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
		var width = 0
		var height = 0

		val offsets = MutableList(measurables.size) { 0 }

		var toolbarNode: Placeable? = null

		val placeables = measurables.mapIndexed { i, measurable ->
			val placeable = measurable.measure(constraints)

			val data = measurable.parentData as? CollapsingToolbarData
			if(data?.markedBody == true) {
				offsets[i] = height
				height += placeable.height
			}else{
				if(toolbarNode != null)
					throw IllegalStateException("there cannot exist multiple toolbars under single parent")

				toolbarNode = placeable
				toolbarHeight.value = placeable.height
			}

			width = max(placeable.width, width)

			placeable
		}

		return layout(
				width.coerceIn(constraints.minWidth, constraints.maxWidth),
				height.coerceIn(constraints.minHeight, constraints.maxHeight)
		) {
			placeables.forEachIndexed { i, placeable ->
				placeable.place(
					0,
					offsets[i] + offsetY.value +
						if(placeable == toolbarNode) 0 else toolbarHeight.value)
			}
		}
	}
}

interface CollapsingToolbarScope {
	fun Modifier.markBody(): Modifier
}

internal class CollapsingToolbarScopeImpl(
	private val nestedScrollConnection: NestedScrollConnection
): CollapsingToolbarScope {
	override fun Modifier.markBody(): Modifier {
		return this
				.then(MarkBodyModifier())
				.nestedScroll(nestedScrollConnection)
	}
}

internal class MarkBodyModifier: ParentDataModifier {
	override fun Density.modifyParentData(parentData: Any?): Any {
		return ((parentData as? CollapsingToolbarData) ?: CollapsingToolbarData(true)).apply {
			markedBody = true
		}
	}
}

private data class CollapsingToolbarData(
	var markedBody: Boolean
)
