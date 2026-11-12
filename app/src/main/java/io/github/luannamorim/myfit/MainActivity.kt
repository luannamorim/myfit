package io.github.luannamorim.myfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.luannamorim.myfit.ui.navigation.MyFitNavHost
import io.github.luannamorim.myfit.ui.theme.MyFitTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MyFitTheme {
                MyFitNavHost()
            }
        }
    }
}
