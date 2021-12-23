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

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.max

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
	collapsingToolbarScaffoldState: CollapsingToolbarScaffoldState,
	content: @Composable AppbarContainerScope.() -> Unit
) {
	AppBarContainer(
		modifier = modifier,
		scrollStrategy = scrollStrategy,
		collapsingToolbarScaffoldState = collapsingToolbarScaffoldState,
		content = content
	)
}

@Deprecated(
	"AppBarContainer is replaced with CollapsingToolbarScaffold",
	replaceWith = ReplaceWith(
		"CollapsingToolbarScaffold",
		"me.onebone.toolbar"
	)
)
@Composable
fun AppBarContainer(
	modifier: Modifier = Modifier,
	scrollStrategy: ScrollStrategy,
	/** The state of a connected collapsing toolbar */
	collapsingToolbarScaffoldState: CollapsingToolbarScaffoldState,
	content: @Composable AppbarContainerScope.() -> Unit
) {
	val flingBehavior = ScrollableDefaults.flingBehavior()
	val snapStrategy = null

	val (scope, measurePolicy) = remember(scrollStrategy, collapsingToolbarScaffoldState) {
		AppbarContainerScopeImpl(scrollStrategy.create(collapsingToolbarScaffoldState, flingBehavior, snapStrategy)) to
				AppbarMeasurePolicy(scrollStrategy, collapsingToolbarScaffoldState)
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

internal class AppbarContainerScopeImpl(
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
	private val collapsingToolbarScaffoldState: CollapsingToolbarScaffoldState
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
					minWidth = 0,
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

		val toolbarState = collapsingToolbarScaffoldState.toolbarState
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

		val offsetY = collapsingToolbarScaffoldState.offsetY
		return layout(
			width.coerceIn(constraints.minWidth, constraints.maxWidth),
			height.coerceIn(constraints.minHeight, constraints.maxHeight)
		) {
			toolbarPlaceable?.place(x = 0, y = offsetY)

			placeables.forEach { placeable ->
				placeable.place(
					x = 0,
					y = offsetY + (toolbarPlaceable?.height ?: 0)
				)
			}
		}
	}
}
