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
            println("Lang code= $code")
            val found = availableLanguages().firstOrNull {
                println("Lang code= $code ${it.langCode}")
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