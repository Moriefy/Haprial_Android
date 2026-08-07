package com.haprial.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.haprial.app.ui.navigation.HaprialNavGraph
import com.haprial.app.ui.theme.HaprialTheme
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.ext.edgeToEdge

class MainActivity : ComponentActivity() {
    @OptIn(UnstableSaltApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        edgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HaprialTheme {
                HaprialNavGraph()
            }
        }
    }
}
