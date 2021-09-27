package me.onebone.toolbar


@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
inline class FabPosition internal constructor(@Suppress("unused") private val value: Int) {
	companion object {

		val Center = FabPosition(0)

		val End = FabPosition(1)
	}

	override fun toString(): String {
		return when (this) {
			Center -> "FabPosition.Center"
			else -> "FabPosition.End"
		}
	}
}