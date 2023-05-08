package me.onebone.toolbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@ExperimentalToolbarApi
@Composable
fun ToolbarWithFabScaffold(
	modifier: Modifier,
	state: CollapsingToolbarScaffoldState,
	scrollStrategy: ScrollStrategy,
	toolbarModifier: Modifier = Modifier,
	toolbarScrollable: Boolean = false,
	toolbar: @Composable CollapsingToolbarScope.() -> Unit,
	fab: @Composable () -> Unit,
	fabPosition: FabPosition = FabPosition.End,
	body: @Composable CollapsingToolbarScaffoldScope.() -> Unit
) {
	SubcomposeLayout(
		modifier = modifier
	) { constraints ->

		val toolbarScaffoldConstraints = constraints.copy(
			minWidth = 0,
			minHeight = 0,
			maxHeight = constraints.maxHeight
		)

		val toolbarScaffoldPlaceables = subcompose(ToolbarWithFabScaffoldContent.ToolbarScaffold) {
			CollapsingToolbarScaffold(
				modifier = modifier,
				state = state,
				scrollStrategy = scrollStrategy,
				toolbarModifier = toolbarModifier,
				toolbarScrollable = toolbarScrollable,
				toolbar = toolbar,
				body = body
			)
		}.map { it.measure(toolbarScaffoldConstraints) }

		val fabConstraints = constraints.copy(
			minWidth = 0,
			minHeight = 0
		)

		val fabPlaceables = subcompose(
			ToolbarWithFabScaffoldContent.Fab,
			fab
		).mapNotNull { measurable ->
			measurable.measure(fabConstraints).takeIf { it.height != 0 && it.width != 0 }
		}

		val fabPlacement = if (fabPlaceables.isNotEmpty()) {
			val fabWidth = fabPlaceables.maxOfOrNull { it.width } ?: 0
			val fabHeight = fabPlaceables.maxOfOrNull { it.height } ?: 0
			// FAB distance from the left of the layout, taking into account LTR / RTL
			val fabLeftOffset = if (fabPosition == FabPosition.End) {
				if (layoutDirection == LayoutDirection.Ltr) {
					constraints.maxWidth - 16.dp.roundToPx() - fabWidth
				} else {
					16.dp.roundToPx()
				}
			} else {
				(constraints.maxWidth - fabWidth) / 2
			}

			FabPlacement(
				left = fabLeftOffset,
				width = fabWidth,
				height = fabHeight
			)
		} else {
			null
		}

		val fabOffsetFromBottom = fabPlacement?.let {
			it.height + 16.dp.roundToPx()
		}

		val width = constraints.maxWidth
		val height = constraints.maxHeight

		layout(width, height) {
			toolbarScaffoldPlaceables.forEach {
				it.place(0, 0)
			}

			fabPlacement?.let { placement ->
				fabPlaceables.forEach {
					it.place(placement.left, height - fabOffsetFromBottom!!)
				}
			}

		}

	}
}

private enum class ToolbarWithFabScaffoldContent {
	ToolbarScaffold, Fab
}
