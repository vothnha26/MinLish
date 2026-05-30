package com.edu.minlish.core.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object ProfileSetup : Screen("profile_setup")
    object PersonalProfile : Screen("personal_profile")
    object WordList : Screen("word_list/{setId}") {
        const val ARG_SET_ID = "setId"
        fun createRoute(setId: String) = "word_list/$setId"
    }
    object Home : Screen("home")
    object Library : Screen("library")
    object Flashcard : Screen("flashcard?setId={setId}") {
        const val ARG_SET_ID = "setId"
        fun createRoute(setId: String?) = if (setId != null) "flashcard?setId=$setId" else "flashcard"
    }
    object Stats : Screen("stats")

    object Settings : Screen("settings")

    object CreateWordSet : Screen("create_word_set")

    object EditWordSet : Screen("edit_word_set/{setId}") {
        const val ARG_SET_ID = "setId"
        fun createRoute(setId: String) = "edit_word_set/$setId"
    }

    object AddWord : Screen("add_word/{setId}") {
        const val ARG_SET_ID = "setId"
        fun createRoute(setId: String) = "add_word/$setId"
    }

    object WordDetail : Screen("word_detail/{wordId}") {
        const val ARG_WORD_ID = "wordId"
        fun createRoute(wordId: String) = "word_detail/$wordId"
    }

    object EditWord : Screen("edit_word/{setId}/{wordId}") {
        const val ARG_SET_ID = "setId"
        const val ARG_WORD_ID = "wordId"
        fun createRoute(setId: String, wordId: String) = "edit_word/$setId/$wordId"
    }
}
