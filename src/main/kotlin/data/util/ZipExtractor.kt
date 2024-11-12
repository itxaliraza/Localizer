package data.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipExtractor {
   suspend fun extractZipFile(filePath: String): MutableMap<String, String> = withContext(Dispatchers.IO){
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