package com.sc.player

import com.sc.player.data.LocaleHelper
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sc.player.ui.AppNav
import com.sc.player.ui.theme.SCPlayerTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SCPlayerTheme { AppNav() }
        }
    }
}
