package data.util

import data.network.NetworkResponse
import data.network.client.NetworkClient
import data.network.client.RequestTypes
import domain.model.LatestRelease
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Asks GitHub for the newest published release so the About dialog can offer an update. */
object UpdateChecker {
    const val REPO_URL = "https://github.com/itxaliraza/Localizer"
    const val RELEASES_URL = "$REPO_URL/releases/latest"
    const val ISSUES_URL = "$REPO_URL/issues"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/itxaliraza/Localizer/releases/latest"

    suspend fun fetchLatest(): NetworkResponse<LatestRelease> {
        val response = NetworkClient.makeStringNetworkRequest(
            LATEST_RELEASE_API,
            RequestTypes.Get,
            headers = mapOf("Accept" to "application/vnd.github+json", "User-Agent" to "FastLocalizer")
        )
        if (response !is NetworkResponse.Success) return NetworkResponse.Failure(response.error ?: "No Internet")
        return try {
            val json = Json.parseToJsonElement(response.data.orEmpty()).jsonObject
            val tag = json.getValue("tag_name").jsonPrimitive.content
            val installer = json["assets"]?.jsonArray
                ?.map { it.jsonObject["browser_download_url"]?.jsonPrimitive?.content.orEmpty() }
                ?.firstOrNull { it.endsWith(".exe", ignoreCase = true) }
            NetworkResponse.Success(
                LatestRelease(
                    version = tag.removePrefix("v"),
                    pageUrl = json["html_url"]?.jsonPrimitive?.content ?: RELEASES_URL,
                    installerUrl = installer
                )
            )
        } catch (e: Exception) {
            NetworkResponse.Failure("Unexpected response from GitHub")
        }
    }

    /** True when [latest] is a higher `MAJOR.MINOR.PATCH` than [current]. Missing parts count as 0. */
    fun isNewer(latest: String, current: String): Boolean {
        val a = latest.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (diff != 0) return diff > 0
        }
        return false
    }
}
