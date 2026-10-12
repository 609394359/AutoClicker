package com.autoclicker.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autoclicker.app.ui.HomeScreen
import com.autoclicker.app.ui.HomeViewModel
import com.autoclicker.app.ui.PermissionGuideScreen
import com.autoclicker.app.ui.theme.AutoClickerTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result doesn't gate MVP functionality */ }

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setContent {
            AutoClickerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(homeViewModel = homeViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions and try to bind to the service. If accessibility is enabled
        // for the first time, this is when we finally get a chance to register our listener.
        homeViewModel.refreshPermissionStatuses()
        val svc = com.autoclicker.app.service.AutoClickService.get()
        svc?.addListener(homeViewModel)
    }
}

@Composable
private fun AppNavHost(homeViewModel: HomeViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenGuide = { nav.navigate("guide") },
                vm = homeViewModel,
            )
        }
        composable("guide") {
            PermissionGuideScreen(onBack = { nav.popBackStack() }, vm = homeViewModel)
        }
    }
}
