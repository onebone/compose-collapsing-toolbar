package me.onebone.toolbar

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable

@Immutable
class SnapStrategy(
	@FloatRange(from = 0.0, to = 1.0) val edge: Float = CollapsingToolbarDefaults.Edge,
	val expandDuration: Int = CollapsingToolbarDefaults.ExpandDuration,
	val collapseDuration: Int = CollapsingToolbarDefaults.CollapseDuration
)