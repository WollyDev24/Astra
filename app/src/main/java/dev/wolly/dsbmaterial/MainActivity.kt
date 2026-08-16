package dev.wolly.dsbmaterial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.wolly.dsbmaterial.ui.MainViewModel
import dev.wolly.dsbmaterial.ui.screens.DSBApp
import dev.wolly.dsbmaterial.ui.theme.DSBMaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val dynamicColor by viewModel.dynamicColor.collectAsState()
            val amoledMode by viewModel.amoledMode.collectAsState()
            val themeIndex by viewModel.themeIndex.collectAsState()
            val useCustomFont by viewModel.useCustomFont.collectAsState()
            val fontRond by viewModel.fontRond.collectAsState()
            DSBMaterialTheme(
                themeIndex = themeIndex,
                dynamicColor = dynamicColor,
                amoledMode = amoledMode,
                useCustomFont = useCustomFont,
                fontRond = fontRond
            ) {
                DSBApp(viewModel)
            }
        }
    }
}
