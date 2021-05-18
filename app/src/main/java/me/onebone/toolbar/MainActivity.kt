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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.onebone.toolbar.ui.theme.CollapsingToolbarTheme

class MainActivity: ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			CollapsingToolbarTheme {
				// A surface container using the 'background' color from the theme
				Surface(color = MaterialTheme.colors.background) {
					MainScreen()
				}
			}
		}
	}
}

@Composable
fun MainScreen() {
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
			// [textSize] is in range of 18..30 depending on [state.progress]
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
				.fillMaxWidth()
				.appBarBody()
		) {
			items(100) {
				Text(
					text = "Item $it",
					modifier = Modifier.padding(8.dp)
				)
			}
		}

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.alpha(0.5f)
				.background(MaterialTheme.colors.secondary)
				.height(40.dp)
				.appBarBody()
		)
	}
}
