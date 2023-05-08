package me.onebone.toolbar

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable

@Immutable
data class SnapConfig(
	@FloatRange(from = 0.0, to = 1.0) val collapseThreshold: Float = 0.5F
)
