package data.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

fun exportLanguageCodesToJson(languageCodes: List<String>,filePath: String="") {
    val path=filePath.ifBlank { System.getProperty("user.home") + "/Downloads/languages_${System.currentTimeMillis()}.txt" }
    val json = Json.encodeToString(languageCodes)
    File(path).writeText(json)
}

fun importLanguageCodesFromJson(filePath: String): List<String> {
    val json = File(filePath).readText()
    return Json.decodeFromString(json)
}