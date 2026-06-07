package com.edu.minlish.core.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object ProfileSetup : Screen("profile_setup?isEdit={isEdit}") {
        const val ARG_IS_EDIT = "isEdit"
        fun createRoute(isEdit: Boolean) = "profile_setup?isEdit=$isEdit"
    }
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

    object Speaking : Screen("speaking")
    object AICreateWordSet : Screen("ai_create_word_set")
    object QuizGame : Screen("quiz_game?setId={setId}&modes={modes}&count={count}") {
        const val ARG_SET_ID = "setId"
        const val ARG_MODES = "modes"
        const val ARG_COUNT = "count"
        fun createRoute(setId: String?, modes: String, count: Int) =
            if (setId != null) "quiz_game?setId=$setId&modes=$modes&count=$count"
            else "quiz_game?modes=$modes&count=$count"
    }
    object GameHub : Screen("game_hub?setId={setId}") {
        const val ARG_SET_ID = "setId"
        fun createRoute(setId: String?) = if (setId != null) "game_hub?setId=$setId" else "game_hub"
    }
    object TranslateAndLookup : Screen("translate_and_lookup")
}
