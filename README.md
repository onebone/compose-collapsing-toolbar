# android-collapsing-toolbar
A simple implementation of collapsing toolbar for Jetpack Compose

## Example
```kotlin
val state = rememberCollapsingToolbarState()

AppbarContainer(
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
		var textSize by remember { mutableStateOf(25.sp) }

		Text(
			text = "Title",
			modifier = Modifier
				.road(Alignment.CenterStart, Alignment.BottomEnd)
				.progress { value ->
					textSize = (18 + (30 - 18) * value).sp
				}
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
AppbarContainer(
	modifier = Modifier
		.fillMaxWidth(),
	collapsingToolbarState = state,
	scrollStrategy = ScrollStrategy.EnterAlways
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
AppbarContainer(
	modifier = Modifier
		.fillMaxWidth(),
	collapsingToolbarState = state,
	scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed
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
AppbarContainer(
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
