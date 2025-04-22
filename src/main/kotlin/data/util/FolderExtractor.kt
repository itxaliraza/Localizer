package data.util

import data.FilesHelper.extractLanguageCode
import data.availableLanguages
import domain.model.LanguageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ExtractionResult(
    val selectedLangs: List<LanguageModel>,
    var extractedFiles: Map<String, String> = mapOf(),
    var changeFileCodes: Map<String, String> = mapOf()
)

object FolderExtractor {


    suspend fun getKeyWithStringsFromFolder(folderPath: String): ExtractionResult =
        withContext(Dispatchers.IO) {
            val extractedFiles = mutableMapOf<String, String>()
            val changeFileCodes = mutableMapOf<String, String>()
            val folder = File(folderPath)

            if (folder.exists() && folder.isDirectory) {
                folder.listFiles()?.forEach { valueFolder ->
                    if (valueFolder.isDirectory && valueFolder.name.startsWith("values")) {
                        val stringsFile = File(valueFolder, "strings.xml")

                        // If strings.xml doesn't exist, create it with default structure
                        if (!stringsFile.exists()) {
                            val defaultContent = """<?xml version="1.0" encoding="utf-8"?>
<resources>
</resources>"""
                            stringsFile.writeText(defaultContent)
                            println("Created missing: ${stringsFile.path}")
                        }

                        // Add it to extractedFiles
                        val content = stringsFile.readText()
                        val relativePath = stringsFile.absolutePath.substringAfter(folderPath)
                        extractedFiles[relativePath.substringAfter("\\").replace("\\", "/")] =
                            content
                    }
                }
            }

            println("Extracted: ${extractedFiles.keys}")

            val selectedLanguages = extractedFiles.mapNotNull { entry ->
                val (rawCode, standardizedCode) = extractLanguageCode(entry.key)

                if (rawCode != standardizedCode) {
                    changeFileCodes[standardizedCode] = rawCode
                }

                val matchedLanguage =
                    availableLanguages.firstOrNull { it.langCode == standardizedCode }
                if (matchedLanguage == null) {
                    println("Not found Lang code= $standardizedCode")
                }

                matchedLanguage
            }

            return@withContext ExtractionResult(
                selectedLangs = selectedLanguages,
                extractedFiles = extractedFiles,
                changeFileCodes = changeFileCodes
            )
        }


}