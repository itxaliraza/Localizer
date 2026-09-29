package domain.model

/** The newest published GitHub release, as reported to the About dialog's update check. */
data class LatestRelease(
    val version: String,
    val pageUrl: String,
    /** Direct link to the Windows installer, or null if the release has no `.exe` attached. */
    val installerUrl: String?,
)
