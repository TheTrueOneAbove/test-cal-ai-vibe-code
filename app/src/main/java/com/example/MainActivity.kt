package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.GeminiNutritionService
import com.example.data.AppDatabase
import com.example.data.FoodRepository
import com.example.ui.CalAiViewModel
import com.example.ui.screens.CameraCaptureScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LogMealBottomSheet
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    DASHBOARD,
    CAMERA_SCANNER
}

class MainActivity : ComponentActivity() {

    private val viewModel: CalAiViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val aiService = GeminiNutritionService()
        val repository = FoodRepository(database.foodDao(), aiService)
        CalAiViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CalAiApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CalAiApp(viewModel: CalAiViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var showLogBottomSheet by remember { mutableStateOf(false) }

    val dailySummary by viewModel.dailySummary.collectAsStateWithLifecycle()
    val dailyEntries by viewModel.dailyEntries.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            currentScreen = AppScreen.CAMERA_SCANNER
        } else {
            Toast.makeText(context, "Camera permission is required to scan food photos.", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCameraFlow() {
        val hasCam = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCam) {
            currentScreen = AppScreen.CAMERA_SCANNER
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Hardware back navigation
    BackHandler(enabled = currentScreen == AppScreen.CAMERA_SCANNER) {
        currentScreen = AppScreen.DASHBOARD
    }

    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
        when (screen) {
            AppScreen.DASHBOARD -> {
                DashboardScreen(
                    dailySummary = dailySummary,
                    entries = dailyEntries,
                    selectedDate = selectedDate,
                    onDateChange = { newTimestamp -> viewModel.setDate(newTimestamp) },
                    onOpenLogger = {
                        viewModel.resetAiState()
                        showLogBottomSheet = true
                    },
                    onDeleteEntry = { entry -> viewModel.deleteMeal(entry) },
                    onUpdateGoals = { c, p, carbs, f -> viewModel.updateGoals(c, p, carbs, f) }
                )

                if (showLogBottomSheet) {
                    LogMealBottomSheet(
                        aiState = aiState,
                        onAnalyzeText = { text -> viewModel.analyzeText(text) },
                        onLaunchCamera = {
                            showLogBottomSheet = false
                            launchCameraFlow()
                        },
                        onSaveMeal = { name, cals, p, c, f, img, notes ->
                            viewModel.logMeal(
                                foodName = name,
                                calories = cals,
                                protein = p,
                                carbs = c,
                                fat = f,
                                imageUrl = img,
                                notes = notes
                            )
                        },
                        onDismiss = {
                            showLogBottomSheet = false
                            viewModel.resetAiState()
                        }
                    )
                }
            }

            AppScreen.CAMERA_SCANNER -> {
                CameraCaptureScreen(
                    onImageCaptured = { bitmap: Bitmap, notes: String?, localPath: String? ->
                        currentScreen = AppScreen.DASHBOARD
                        showLogBottomSheet = true
                        viewModel.analyzeImage(bitmap, notes, localPath)
                    },
                    onClose = {
                        currentScreen = AppScreen.DASHBOARD
                    }
                )
            }
        }
    }
}
