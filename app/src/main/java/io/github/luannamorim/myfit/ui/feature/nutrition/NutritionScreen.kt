package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.ui.components.TabScreen

@Composable
fun NutritionScreen(modifier: Modifier = Modifier) {
    TabScreen(title = stringResource(R.string.nav_nutrition), modifier = modifier) {}
}
