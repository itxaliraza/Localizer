package data.util

import data.FilesHelper.extractLanguageCode
import data.availableLanguages
import domain.model.LanguageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ExtractionResult(
    val selectedLangs: List<LanguageModel>,
    var extractedFiles: Map<String, String> = mapOf()
)

object FolderExtractor {


    suspend fun getKeyWithStringsFromFolder(folderPath: String): ExtractionResult= withContext(Dispatchers.IO) {
        val extractedFiles = mutableMapOf<String, String>()
        val folder = File(folderPath)

        if (folder.exists() && folder.isDirectory) {
            // Get all files in the directory
            folder.listFiles()?.forEach { folder ->
                // Process only strings.xml files
                folder.listFiles()?.forEach {file->
                    if (file.isFile && file.name.endsWith("strings.xml")) {
                        val content = file.readText()
                        val relativePath =   file.absolutePath.substringAfter(folderPath)

                        extractedFiles[relativePath.substringAfter("\\").replace("\\","/")] = content
                    }
                }
            }
        }
        println("extracted"+extractedFiles.keys)
        val  selectedLanguages = extractedFiles.mapNotNull { entry ->
            val code = extractLanguageCode(entry.key)
             val found = availableLanguages().firstOrNull {
                 (it.langCode == code )
            }
            if (found == null) {
                println("Not found Lang code= $code")
            }
            found
        }
        return@withContext ExtractionResult(selectedLangs = selectedLanguages,extractedFiles=extractedFiles)
    }

}