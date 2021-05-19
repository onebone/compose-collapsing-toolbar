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

@Deprecated(
	"Use AppBarContainer for naming consistency",
	replaceWith = ReplaceWith(
		"AppBarContainer(modifier, scrollStrategy, collapsingToolbarState, content)",
		"me.onebone.toolbar"
	)
)
@Composable
fun AppbarContainer(
	modifier: Modifier = Modifier,
	scrollStrategy: ScrollStrategy,
	collapsingToolbarState: CollapsingToolbarState,
	content: @Composable AppbarContainerScope.() -> Unit
) {
	AppBarContainer(
		modifier = modifier,
		scrollStrategy = scrollStrategy,
		collapsingToolbarState = collapsingToolbarState,
		content = content
	)
}

@Composable
fun AppBarContainer(
	modifier: Modifier = Modifier,
	scrollStrategy: ScrollStrategy,
	/** The state of a connected collapsing toolbar */
	collapsingToolbarState: CollapsingToolbarState,
	content: @Composable AppbarContainerScope.() -> Unit
) {
	val offsetY = remember { mutableStateOf(0) }

	val (scope, measurePolicy) = remember(scrollStrategy, collapsingToolbarState) {
		AppbarContainerScopeImpl(scrollStrategy.create(offsetY, collapsingToolbarState)) to
				AppbarMeasurePolicy(scrollStrategy, collapsingToolbarState, offsetY)
	}

	Layout(
		content = { scope.content() },
		measurePolicy = measurePolicy,
		modifier = modifier
	)
}

interface AppbarContainerScope {
	fun Modifier.appBarBody(): Modifier
}

enum class ScrollStrategy {
	EnterAlways {
		override fun create(
			offsetY: MutableState<Int>,
			toolbarState: CollapsingToolbarState
		): NestedScrollConnection =
			EnterAlwaysNestedScrollConnection(offsetY, toolbarState)
	},
	EnterAlwaysCollapsed {
		override fun create(
			offsetY: MutableState<Int>,
			toolbarState: CollapsingToolbarState
		): NestedScrollConnection =
			EnterAlwaysCollapsedNestedScrollConnection(offsetY, toolbarState)
	},
	ExitUntilCollapsed {
		override fun create(
			offsetY: MutableState<Int>,
			toolbarState: CollapsingToolbarState
		): NestedScrollConnection =
			ExitUntilCollapsedNestedScrollConnection(toolbarState)
	};

	internal abstract fun create(
		offsetY: MutableState<Int>,
		toolbarState: CollapsingToolbarState
	): NestedScrollConnection
}

internal class EnterAlwaysNestedScrollConnection(
	private val offsetY: MutableState<Int>,
	private val toolbarState: CollapsingToolbarState
): NestedScrollConnection {
	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		val dy = available.y

		val toolbar = toolbarState.height.toFloat()
		val offset = offsetY.value.toFloat()

		// -toolbarHeight <= offsetY + dy <= 0
		val consume = if(dy < 0) {
			val toolbarConsumption = toolbarState.feedScroll(dy)
			val remaining = dy - toolbarConsumption
			val offsetConsumption = remaining.coerceAtLeast(-toolbar - offset)
			offsetY.value += offsetConsumption.roundToInt()

			toolbarConsumption + offsetConsumption
		}else{
			val offsetConsumption = dy.coerceAtMost(-offset)
			offsetY.value += offsetConsumption.roundToInt()

			val toolbarConsumption = toolbarState.feedScroll(dy - offsetConsumption)

			offsetConsumption + toolbarConsumption
		}

		return Offset(0f, consume)
	}
}

internal class EnterAlwaysCollapsedNestedScrollConnection(
	private val offsetY: MutableState<Int>,
	private val toolbarState: CollapsingToolbarState
): NestedScrollConnection {
	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		val dy = available.y

		val consumed = if(dy > 0) { // expanding: offset -> body -> toolbar
			val offsetConsumption = dy.coerceAtMost(-offsetY.value.toFloat())
			offsetY.value += offsetConsumption.roundToInt()

			offsetConsumption
		}else{ // collapsing: toolbar -> offset -> body
			val toolbarConsumption = toolbarState.feedScroll(dy)
			val offsetConsumption = (dy - toolbarConsumption).coerceAtLeast(-toolbarState.height.toFloat() - offsetY.value)

			offsetY.value += offsetConsumption.roundToInt()

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
			Offset(0f, toolbarState.feedScroll(dy))
		}else{
			Offset(0f, 0f)
		}
	}
}

internal class ExitUntilCollapsedNestedScrollConnection(
	private val toolbarState: CollapsingToolbarState
): NestedScrollConnection {
	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		val dy = available.y
		val consume = if(dy < 0) { // collapsing: toolbar -> body
			toolbarState.feedScroll(dy)
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
			toolbarState.feedScroll(dy)
		}else{
			0f
		}

		return Offset(0f, consume)
	}
}

private class AppbarContainerScopeImpl(
	private val nestedScrollConnection: NestedScrollConnection
): AppbarContainerScope {
	override fun Modifier.appBarBody(): Modifier {
		return this
			.then(AppBarBodyMarkerModifier)
			.nestedScroll(nestedScrollConnection)
	}
}

private object AppBarBodyMarkerModifier: ParentDataModifier {
	override fun Density.modifyParentData(parentData: Any?): Any {
		return AppBarBodyMarker
	}
}

private object AppBarBodyMarker

private class AppbarMeasurePolicy(
	private val scrollStrategy: ScrollStrategy,
	private val toolbarState: CollapsingToolbarState,
	private val offsetY: State<Int>
): MeasurePolicy {
	override fun MeasureScope.measure(
		measurables: List<Measurable>,
		constraints: Constraints
	): MeasureResult {
		var width = 0
		var height = 0

		var toolbarPlaceable: Placeable? = null

		val nonToolbars = measurables.filter {
			val data = it.parentData
			if(data != AppBarBodyMarker) {
				if(toolbarPlaceable != null)
					throw IllegalStateException("There cannot exist multiple toolbars under single parent")

				val placeable = it.measure(constraints.copy(
					minHeight = 0
				))
				width = max(width, placeable.width)
				height = max(height, placeable.height)

				toolbarPlaceable = placeable

				false
			}else{
				true
			}
		}

		val placeables = nonToolbars.map { measurable ->
			val childConstraints = if(scrollStrategy == ScrollStrategy.ExitUntilCollapsed) {
				constraints.copy(
					minWidth = 0,
					minHeight = 0,
					maxHeight = max(0, constraints.maxHeight - toolbarState.minHeight)
				)
			}else{
				constraints.copy(
					minWidth = 0,
					minHeight = 0
				)
			}

			val placeable = measurable.measure(childConstraints)

			width = max(width, placeable.width)
			height = max(height, placeable.height)

			placeable
		}

		height += (toolbarPlaceable?.height ?: 0)

		return layout(
			width.coerceIn(constraints.minWidth, constraints.maxWidth),
			height.coerceIn(constraints.minHeight, constraints.maxHeight)
		) {
			toolbarPlaceable?.place(x = 0, y = offsetY.value)

			placeables.forEach { placeable ->
				placeable.place(
					x = 0,
					y = offsetY.value + (toolbarPlaceable?.height ?: 0)
				)
			}
		}
	}
}
