package com.mohamedrejeb.richeditor.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.mohamedrejeb.richeditor.sample.common.App

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemUiController = rememberSystemUiController()
            val color = MaterialTheme.colors.primaryVariant
            MaterialTheme {
                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = color,
                        darkIcons = false
                    )
                }
                App()
            }
        }
    }
}