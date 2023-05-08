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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.onebone.toolbar.ui.theme.CollapsingToolbarTheme

class ParallaxActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			CollapsingToolbarTheme {
				Surface(color = MaterialTheme.colors.background) {
					ParallaxEffect()
				}
			}
		}
	}
}

@Composable
fun ParallaxEffect() {
	val state = rememberCollapsingToolbarScaffoldState()

	var enabled by remember { mutableStateOf(true) }

	Box {
		CollapsingToolbarScaffold(
			modifier = Modifier.fillMaxSize(),
			state = state,
			scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed,
			snapConfig = SnapConfig(),
			toolbarModifier = Modifier.background(MaterialTheme.colors.primary),
			enabled = enabled,
			toolbar = {
				// Collapsing toolbar collapses its size as small as the that of
				// a smallest child. To make the toolbar collapse to 50dp, we create
				// a dummy Spacer composable.
				// You may replace it with TopAppBar or other preferred composable.
				Spacer(
					modifier = Modifier
						.fillMaxWidth()
						.height(50.dp)
				)

				Image(
					painter = painterResource(id = R.drawable.android),
					modifier = Modifier
						.parallax(0.5f)
						.height(300.dp)
						.graphicsLayer {
							// change alpha of Image as the toolbar expands
							alpha = state.toolbarState.progress
						},
					contentScale = ContentScale.Crop,
					contentDescription = null
				)
			}
		) {
			LazyColumn(
				modifier = Modifier
					.fillMaxSize()
			) {
				items(
					List(100) { "Hello World!! $it" }
				) {
					Text(
						text = it,
						modifier = Modifier
							.fillMaxWidth()
							.padding(4.dp)
					)
				}
			}

			@OptIn(ExperimentalToolbarApi::class)
			Button(
				modifier = Modifier
					.padding(16.dp)
					.align(Alignment.BottomEnd),
				onClick = { }
			) {
				Text(text = "Floating Button!")
			}
		}

		Row(
			verticalAlignment = Alignment.CenterVertically
		) {
			Checkbox(checked = enabled, onCheckedChange = { enabled = !enabled })

			Text("Enable collapse/expand", fontWeight = FontWeight.Bold)
		}
	}
}
