package data.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

fun exportLanguageCodesToJson(filePath: String, languageCodes: List<String>) {
    val json = Json.encodeToString(languageCodes)
    File(filePath).writeText(json)
}

fun importLanguageCodesFromJson(filePath: String): List<String> {
    val json = File(filePath).readText()
    return Json.decodeFromString(json)
}