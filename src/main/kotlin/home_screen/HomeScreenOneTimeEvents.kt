package home_screen

sealed interface HomeScreenOneTimeEvents {
    data object FileLoadedSuccess:HomeScreenOneTimeEvents
    data object FileLoadedFail:HomeScreenOneTimeEvents

}