# compose-collapsing-toolbar
A simple implementation of collapsing toolbar for Jetpack Compose

## Installation
```gradle
implementation "me.onebone:toolbar-compose:2.0.0"
```

## Example
```kotlin
val state = rememberCollapsingToolbarState()

AppBarContainer(
	modifier = Modifier
		.fillMaxWidth(),
	collapsingToolbarState = state,
	scrollStrategy = ScrollStrategy.ExitUntilCollapsed
) {
	CollapsingToolbar(
		modifier = Modifier
			.background(MaterialTheme.colors.primary),
		collapsingToolbarState = state
	) {
		val textSize = (18 + (30 - 18) * state.progress).sp

		Text(
			text = "Title",
			modifier = Modifier
				.road(Alignment.CenterStart, Alignment.BottomEnd)
				.padding(60.dp, 16.dp, 16.dp, 16.dp),
			color = Color.White,
			fontSize = textSize
		)

		Image(
			modifier = Modifier
				.pin()
				.padding(16.dp),
			painter = painterResource(id = R.drawable.abc_vector_test),
			contentDescription = null
		)

		Box(
			modifier = Modifier
				.height(150.dp)
				.pin()
		)
	}

	LazyColumn(
		modifier = Modifier
			.appBarBody()
	) {
		items(100) {
			Text(
				text = "Item $it",
				modifier = Modifier.padding(8.dp)
			)
		}
	}
}
```

## Scroll Strategy
### ScrollStrategy.EnterAlways
![EnterAlways](img/enter-always.gif)

```kotlin
AppBarContainer(
	modifier = Modifier
		.fillMaxWidth(),
	collapsingToolbarState = state,
	scrollStrategy = ScrollStrategy.EnterAlways // <---
) {
	CollapsingToolbar(
		modifier = Modifier
			.background(MaterialTheme.colors.primary),
		collapsingToolbarState = state
	) {
		// ...
	}
	
	LazyColumn(
		modifier = Modifier
			.appBarBody()
	) {
		// ...
	}
}
```

### ScrollStrategy.EnterAlwaysCollapsed
![EnterAlwaysCollapsed](img/enter-always-collapsed.gif)

```kotlin
AppBarContainer(
	modifier = Modifier
		.fillMaxWidth(),
	collapsingToolbarState = state,
	scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed // <---
) {
	CollapsingToolbar(
		modifier = Modifier
			.background(MaterialTheme.colors.primary),
		collapsingToolbarState = state
	) {
		// ...
	}
	
	LazyColumn(
		modifier = Modifier
			.appBarBody()
	) {
		// ...
	}
}
```

### ScrollStrategy.ExitUntilCollapsed
![ExitUntilCollapsed](img/exit-until-collapsed.gif)

```kotlin
AppBarContainer(
	modifier = Modifier
		.fillMaxWidth(),
	collapsingToolbarState = state,
	scrollStrategy = ScrollStrategy.ExitUntilCollapsed // <---
) {
	CollapsingToolbar(
		modifier = Modifier
			.background(MaterialTheme.colors.primary),
		collapsingToolbarState = state
	) {
		// ...
	}
	
	LazyColumn(
		modifier = Modifier
			.appBarBody()
	) {
		// ...
	}
}
```
