package com.edu.minlish

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.edu.minlish.core.designsystem.component.MinLishBottomNav
import com.edu.minlish.core.navigation.Screen
import com.edu.minlish.features.onboarding.presentation.SplashScreen
import com.edu.minlish.features.onboarding.presentation.OnboardingScreen
import com.edu.minlish.features.auth.presentation.LoginScreen
import com.edu.minlish.features.auth.presentation.RegisterScreen
import com.edu.minlish.features.auth.presentation.ForgotPasswordScreen
import com.edu.minlish.features.profilesetup.presentation.ProfileSetupScreen
import com.edu.minlish.features.profilesetup.presentation.PersonalProfileScreen
import com.edu.minlish.features.home.presentation.HomeScreen
import com.edu.minlish.features.library.presentation.LibraryScreen
import com.edu.minlish.features.library.presentation.CreateWordSetScreen
import com.edu.minlish.features.library.presentation.AICreateWordSetScreen
import com.edu.minlish.features.library.presentation.AddWordScreen
import com.edu.minlish.features.library.presentation.WordListScreen
import com.edu.minlish.features.library.presentation.TranslateAndLookupScreen
import com.edu.minlish.features.settings.presentation.SettingsScreen
import com.edu.minlish.features.stats.presentation.StatsScreen
import com.edu.minlish.features.learning.presentation.FlashcardScreen
import com.edu.minlish.features.learning.presentation.WordDetailScreen
import com.edu.minlish.features.learning.presentation.QuizGameScreen
import com.edu.minlish.features.learning.presentation.GameHubScreen
import com.edu.minlish.features.notification.presentation.NotificationListScreen
import com.edu.minlish.features.notification.presentation.AdminNotificationScreen
import com.edu.minlish.features.speaking.presentation.SpeakingScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.remember

@Composable
fun MinLishApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = androidx.compose.ui.platform.LocalContext.current
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val performLogout = {
        // Đăng xuất Firebase
        FirebaseAuth.getInstance().signOut()
        // Xóa cache dữ liệu học tập
        com.edu.minlish.core.util.SessionDataManager.clear()
        // Đăng xuất Google SDK (xóa tài khoản đã chọn) và chuyển về Login
        googleSignInClient.signOut().addOnCompleteListener {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Define routes that should display bottom navigation bar
    val showBottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Library.route,
        Screen.Stats.route,
        Screen.PersonalProfile.route,
        Screen.TranslateAndLookup.route,
        Screen.WordList.route,
        Screen.WordDetail.route
    )

    val shouldShowBottomBar = currentRoute != null && showBottomBarRoutes.any { route ->
        if (route.contains("{")) {
            val baseRoute = route.substringBefore("{")
            currentRoute.startsWith(baseRoute)
        } else {
            currentRoute == route
        }
    }
    
    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                MinLishBottomNav(
                    currentRoute = currentRoute,
                    onTabClick = { screen ->
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Splash Screen Route
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToProfileSetup = {
                        navController.navigate(Screen.ProfileSetup.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Onboarding Screen Route
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onDone = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Login Screen Route
            composable(Screen.Login.route) {
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    onLogin = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onProfileSetup = {
                        navController.navigate(Screen.ProfileSetup.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onForgotPassword = {
                        navController.navigate(Screen.ForgotPassword.route)
                    }
                )
            }

            // Register Screen Route
            composable(Screen.Register.route) {
                RegisterScreen(
                    onBack = { navController.popBackStack() },
                    onLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onRegister = {
                        navController.navigate(Screen.ProfileSetup.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                )
            }

            // Forgot Password Screen Route
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onBack = { navController.popBackStack() },
                    onLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                        }
                    }
                )
            }

            // Profile Setup Screen Route
            composable(
                route = Screen.ProfileSetup.route,
                arguments = listOf(
                    navArgument(Screen.ProfileSetup.ARG_IS_EDIT) {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val isEdit = backStackEntry.arguments?.getBoolean(Screen.ProfileSetup.ARG_IS_EDIT) ?: false
                ProfileSetupScreen(
                    isEdit = isEdit,
                    onDone = {
                        if (isEdit) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // Personal Profile Screen Route
            composable(Screen.PersonalProfile.route) {
                PersonalProfileScreen(
                    onLogout = { performLogout() },
                    onEditProfile = {
                        navController.navigate(Screen.ProfileSetup.createRoute(isEdit = true))
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onNavigateToAdminNotifications = {
                        navController.navigate(Screen.AdminNotifications.route)
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            // Home Screen Route
            composable(Screen.Home.route) {
                HomeScreen(
                    onStartLearning = {
                        navController.navigate(Screen.Flashcard.createRoute(null))
                    },
                    onPracticeSpeaking = {
                        navController.navigate(Screen.Speaking.route)
                    },
                    onPlayQuiz = {
                        navController.navigate(Screen.GameHub.createRoute(null))
                    },
                    onWordClick = { word ->
                        navController.navigate(Screen.WordDetail.createRoute(word))
                    },
                    onNavigateToTranslate = {
                        navController.navigate(Screen.TranslateAndLookup.route)
                    }
                )
            }

            // Library Screen Route
            composable(Screen.Library.route) {
                LibraryScreen(
                    onWordSetClick = { setId ->
                        navController.navigate(Screen.WordList.createRoute(setId))
                    },
                    onCreateWordSetClick = {
                        navController.navigate(Screen.CreateWordSet.route)
                    },
                    onAICreateWordSetClick = {
                        navController.navigate(Screen.AICreateWordSet.route)
                    },
                    onAddWordClick = { setId ->
                        navController.navigate(Screen.AddWord.createRoute(setId))
                    }
                )
            }

            // Word List Screen Route
            composable(
                route = Screen.WordList.route,
                arguments = listOf(
                    navArgument(Screen.WordList.ARG_SET_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val setId = backStackEntry.arguments?.getString(Screen.WordList.ARG_SET_ID) ?: ""
                WordListScreen(
                    setId = setId,
                    onBack = { navController.popBackStack() },
                    onWordClick = { wordId ->
                        navController.navigate(Screen.WordDetail.createRoute(wordId))
                    },
                    onAddWord = {
                        navController.navigate(Screen.AddWord.createRoute(setId))
                    },
                    onStudyClick = { targetSetId ->
                        navController.navigate(Screen.Flashcard.createRoute(targetSetId))
                    },
                    onEditSetClick = { targetSetId ->
                        navController.navigate(Screen.EditWordSet.createRoute(targetSetId))
                    }
                )
            }

            // Create Word Set Route
            composable(Screen.CreateWordSet.route) {
                CreateWordSetScreen(
                    onBack = { navController.popBackStack() },
                    onCreateSuccess = { navController.popBackStack() }
                )
            }

            // AI Create Word Set Route
            composable(Screen.AICreateWordSet.route) {
                AICreateWordSetScreen(
                    onBack = { navController.popBackStack() },
                    onCreateSuccess = { navController.popBackStack() }
                )
            }

            // Edit Word Set Route
            composable(
                route = Screen.EditWordSet.route,
                arguments = listOf(
                    navArgument(Screen.EditWordSet.ARG_SET_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val setId = backStackEntry.arguments?.getString(Screen.EditWordSet.ARG_SET_ID) ?: ""
                CreateWordSetScreen(
                    setId = setId,
                    onBack = { navController.popBackStack() },
                    onCreateSuccess = {
                        navController.popBackStack(Screen.Library.route, false)
                    },
                    onDeleteSuccess = {
                        navController.popBackStack(Screen.Library.route, false)
                    }
                )
            }

            // Add Word Route
            composable(
                route = Screen.AddWord.route,
                arguments = listOf(
                    navArgument(Screen.AddWord.ARG_SET_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val setId = backStackEntry.arguments?.getString(Screen.AddWord.ARG_SET_ID) ?: ""
                AddWordScreen(
                    setId = setId,
                    onBack = { navController.popBackStack() },
                    onAddSuccess = { navController.popBackStack() }
                )
            }

            // Stats Screen Route
            composable(Screen.Stats.route) {
                StatsScreen()
            }

            // Settings Screen Route
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = { performLogout() }
                )
            }

            // Flashcard Screen Route
            composable(
                route = Screen.Flashcard.route,
                arguments = listOf(
                    navArgument(Screen.Flashcard.ARG_SET_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val setId = backStackEntry.arguments?.getString(Screen.Flashcard.ARG_SET_ID)
                FlashcardScreen(
                    setId = setId,
                    onBack = { navController.popBackStack() },
                    onPlayQuiz = { targetSetId ->
                        navController.navigate(Screen.GameHub.createRoute(targetSetId)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            // Word Detail Screen Route
            composable(
                route = Screen.WordDetail.route,
                arguments = listOf(
                    navArgument(Screen.WordDetail.ARG_WORD_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val wordId = backStackEntry.arguments?.getString(Screen.WordDetail.ARG_WORD_ID) ?: ""
                WordDetailScreen(
                    wordId = wordId,
                    onBack = { navController.popBackStack() },
                    onEditClick = { setId, targetWordId ->
                        navController.navigate(Screen.EditWord.createRoute(setId, targetWordId))
                    }
                )
            }

            // Edit Word Route
            composable(
                route = Screen.EditWord.route,
                arguments = listOf(
                    navArgument(Screen.EditWord.ARG_SET_ID) { type = NavType.StringType },
                    navArgument(Screen.EditWord.ARG_WORD_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val setId = backStackEntry.arguments?.getString(Screen.EditWord.ARG_SET_ID) ?: ""
                val wordId = backStackEntry.arguments?.getString(Screen.EditWord.ARG_WORD_ID) ?: ""
                AddWordScreen(
                    setId = setId,
                    wordId = wordId,
                    onBack = { navController.popBackStack() },
                    onAddSuccess = { navController.popBackStack() }
                )
            }

            // User Notifications Route
            composable(Screen.Notifications.route) {
                NotificationListScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Admin Notifications Route
            composable(Screen.AdminNotifications.route) {
                AdminNotificationScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Speaking Route
            composable(Screen.Speaking.route) {
                SpeakingScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Quiz Game Route
            composable(
                route = Screen.QuizGame.route,
                arguments = listOf(
                    navArgument(Screen.QuizGame.ARG_SET_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(Screen.QuizGame.ARG_MODES) {
                        type = NavType.StringType
                        defaultValue = "MULTIPLE_CHOICE"
                    },
                    navArgument(Screen.QuizGame.ARG_COUNT) {
                        type = NavType.IntType
                        defaultValue = 10
                    }
                )
            ) { backStackEntry ->
                val setId = backStackEntry.arguments?.getString(Screen.QuizGame.ARG_SET_ID)
                val modes = backStackEntry.arguments?.getString(Screen.QuizGame.ARG_MODES) ?: "MULTIPLE_CHOICE"
                val count = backStackEntry.arguments?.getInt(Screen.QuizGame.ARG_COUNT) ?: 10
                QuizGameScreen(
                    setId = setId,
                    modes = modes,
                    questionCount = count,
                    onBack = { navController.popBackStack() }
                )
            }

            // Game Hub Route
            composable(
                route = Screen.GameHub.route,
                arguments = listOf(
                    navArgument(Screen.GameHub.ARG_SET_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val setId = backStackEntry.arguments?.getString(Screen.GameHub.ARG_SET_ID)
                GameHubScreen(
                    setId = setId,
                    onBack = { navController.popBackStack() },
                    onStartGame = { selectedModes, selectedCount ->
                        navController.navigate(Screen.QuizGame.createRoute(setId, selectedModes, selectedCount))
                    }
                )
            }

            // Translate And Lookup Route
            composable(Screen.TranslateAndLookup.route) {
                TranslateAndLookupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
