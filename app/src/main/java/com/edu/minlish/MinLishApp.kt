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
import com.edu.minlish.features.library.presentation.AddWordScreen
import com.edu.minlish.features.settings.presentation.SettingsScreen
import com.edu.minlish.features.stats.presentation.StatsScreen
import com.edu.minlish.features.learning.presentation.FlashcardScreen
import com.edu.minlish.features.learning.presentation.WordDetailScreen

@Composable
fun MinLishApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define routes that should display bottom navigation bar
    val showBottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Library.route,
        Screen.Stats.route,
        Screen.PersonalProfile.route
    )

    val shouldShowBottomBar = currentRoute in showBottomBarRoutes

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
                    onDone = {
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
            composable(Screen.ProfileSetup.route) {
                ProfileSetupScreen(
                    onDone = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                        }
                    }
                )
            }

            // Personal Profile Screen Route
            composable(Screen.PersonalProfile.route) {
                PersonalProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0)
                        }
                    },
                    onEditProfile = {
                        navController.navigate(Screen.ProfileSetup.route)
                    }
                )
            }

            // Home Screen Route
            composable(Screen.Home.route) {
                HomeScreen(
                    onStartLearning = {
                        navController.navigate(Screen.Flashcard.route)
                    },
                    onWordClick = { word ->
                        navController.navigate(Screen.WordDetail.createRoute(word))
                    }
                )
            }

            // Library Screen Route
            composable(Screen.Library.route) {
                LibraryScreen(
                    onWordSetClick = { _ ->
                        navController.navigate(Screen.WordDetail.createRoute("IELTS Academic"))
                    },
                    onCreateWordSetClick = {
                        navController.navigate(Screen.CreateWordSet.route)
                    },
                    onAddWordClick = { setId ->
                        navController.navigate(Screen.AddWord.createRoute(setId))
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
                StatsScreen(
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            // Settings Screen Route
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            // Flashcard Screen Route
            composable(Screen.Flashcard.route) {
                FlashcardScreen(
                    onBack = { navController.popBackStack() }
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
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
