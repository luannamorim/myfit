package io.github.luannamorim.myfit.ui.feature.routines

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.ui.components.EmptyState
import io.github.luannamorim.myfit.ui.components.TabScreen

@Composable
fun RoutinesScreen(modifier: Modifier = Modifier) {
    TabScreen(title = stringResource(R.string.nav_routines), modifier = modifier) {
        EmptyState(title = "Em breve", message = "Esta área está sendo construída.")
    }
}
