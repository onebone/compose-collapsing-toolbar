package me.onebone.toolbar

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable

@Immutable
class SnapStrategy(
	@FloatRange(from = 0.0, to = 1.0) val edge: Float = CollapsingToolbarDefaults.EDGE,
	val expandDuration: Int = CollapsingToolbarDefaults.EXPAND_DURATION,
	val collapseDuration: Int = CollapsingToolbarDefaults.COLLAPSE_DURATION
)