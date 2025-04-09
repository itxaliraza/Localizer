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

object ZipExtractor {


    suspend fun extractZipFile(path: String): ExtractionResult {
        var extractedFiles: Map<String, String> = mapOf()
        var selectedLanguages: List<LanguageModel> = emptyList()

        if (path.endsWith("strings.xml")) {
            val content =
                File(path).readText() // or use zis.readBytes().decodeToString() for a ZipInputStream

            extractedFiles = mapOf(
                "values/strings.xml"
                        to
                        content
            )
        } else if (path.endsWith("zip", ignoreCase = true)) {
            extractedFiles =
                getKeyWithStringsContent(path)
            println("extracted zip = ${extractedFiles.keys}")
            selectedLanguages = extractedFiles.mapNotNull { entry ->
                val code = extractLanguageCode(entry.key)
                println("Lang code= $code")
                val found = availableLanguages().firstOrNull {
                    (it.langCode == code  )
                }
                if (found == null) {
                    println("Not found Lang code= $code")
                }
                found
            }
        }

        return ExtractionResult(
            selectedLangs = selectedLanguages,
            extractedFiles = extractedFiles
        )
    }

    suspend fun getKeyWithStringsContent(filePath: String): MutableMap<String, String> = withContext(Dispatchers.IO){
       val stringFilesList = mutableMapOf<String, String>()
       if (filePath.endsWith(".zip")) {

           FileInputStream(filePath).use { fis ->
               ZipInputStream(fis).use { zis ->
                   var entry: ZipEntry? = zis.nextEntry
                   while (entry != null) {
                       if (entry.name.endsWith("strings.xml")) {
                           val content = zis.readBytes().decodeToString()
                           val relativePath = if ("values" in entry.name) {
                               "values" + entry.name.substringAfterLast("values")
                           } else {
                               entry.name
                           }
                           stringFilesList[relativePath] = content
                       }
                       entry = zis.nextEntry
                   }
                   zis.closeEntry()
               }
           }
       }
        return@withContext stringFilesList
    }
}