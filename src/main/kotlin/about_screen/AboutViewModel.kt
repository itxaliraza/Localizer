package about_screen

import buildinfo.BuildInfo
import data.network.NetworkResponse
import data.util.UpdateChecker
import domain.model.LatestRelease
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val latestVersion: String) : UpdateState
    data class Available(val release: LatestRelease) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/** Backs the About dialog: exposes the running version and checks GitHub for a newer release. */
class AboutViewModel {
    val currentVersion: String = BuildInfo.VERSION

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState = _updateState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun checkForUpdates() {
        if (_updateState.value is UpdateState.Checking) return
        _updateState.value = UpdateState.Checking
        scope.launch {
            _updateState.value = when (val response = UpdateChecker.fetchLatest()) {
                is NetworkResponse.Success -> {
                    val release = response.data!!
                    if (UpdateChecker.isNewer(release.version, currentVersion)) UpdateState.Available(release)
                    else UpdateState.UpToDate(release.version)
                }

                else -> UpdateState.Failed(response.error ?: "No Internet")
            }
        }
    }

    /** Where "Download" should go: the installer itself when attached, otherwise the release page. */
    fun downloadUrl(release: LatestRelease): String = release.installerUrl ?: release.pageUrl

    val repoUrl = UpdateChecker.REPO_URL
    val releasesUrl = UpdateChecker.RELEASES_URL
    val issuesUrl = UpdateChecker.ISSUES_URL
}
