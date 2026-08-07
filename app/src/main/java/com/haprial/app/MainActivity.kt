package com.haprial.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.haprial.app.ui.navigation.HaprialNavGraph
import com.haprial.app.ui.theme.HaprialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HaprialTheme {
                HaprialNavGraph()
            }
        }
    }
}
