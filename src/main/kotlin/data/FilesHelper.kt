package data

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.writeText


object FilesHelper {


    fun getFilesXmlContents(fileContents: Map<String, String>): Map<String, FileXmlData> {
        return fileContents.mapValues { (fileName, content) ->
            val currentPairs = parseXml(content)
            FileXmlData(
                contents = content,
                keyValuePairs = currentPairs,
                languageCode = extractLanguageCode(fileName)
            )
        }
    }

    fun extractLanguageCode(fileName: String): String {
        val regex = Regex("""values-([\w-]+)/""")
        return regex.find(fileName)?.groups?.get(1)?.value ?: "en"
    }

    fun parseXml(xmlContent: String): Map<String, String> {
        val keyValuePairs = mutableMapOf<String, String>()
        val root = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xmlContent.toByteArray()))
            .documentElement

        val nodeList = root.getElementsByTagName("string")
        for (i in 0 until nodeList.length) {
            val stringElement = nodeList.item(i) as Element
            val key = stringElement.getAttribute("name")
            val value = stringElement.textContent
            val translatable: String = stringElement.getAttribute("translatable")
            println("key = $key, value = $value, transl= $translatable")
            if (key != null && value != null && translatable != "false") {
                keyValuePairs[key] = value
            }
        }
        return keyValuePairs
    }

    fun combineStringsWithLimit(strings: List<String>, separator: String, limit: Int = 3600): List<String> {
        val combinedStrings = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (s in strings) {
            if (currentChunk.length + separator.length + s.length > limit) {
                combinedStrings.add(currentChunk.toString())
                currentChunk = StringBuilder(s)
            } else {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append(separator)
                }
                currentChunk.append(s)
            }
        }
        if (currentChunk.isNotEmpty()) {
            combinedStrings.add(currentChunk.toString())
        }

        return combinedStrings
    }

    fun writeXmlToFile(xmlContent: String, filePath: String) {
        try {
            val file = File(filePath)
            file.parentFile.mkdirs()
            file.writeText(xmlContent)
            println("XML written to $filePath")
        } catch (e: Exception) {
            println("An error occurred while writing: ${e.message}")
        }
    }

    fun addNewEntriesToXml(xmlContent: String, newEntries: Map<String, String>): String {
        try {
            val doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(ByteArrayInputStream(xmlContent.toByteArray()))
            val root = doc.documentElement

            if (root.tagName != "resources") {
                throw IllegalArgumentException("Not valid XML")
            }

            for ((key, value) in newEntries) {
                val newString = doc.createElement("string")
                newString.setAttribute("name", key)
                newString.textContent = value
                root.appendChild(newString)
            }

            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")

            val result = StreamResult(StringWriter())
            transformer.transform(DOMSource(doc), result)
            return result.writer.toString()

        } catch (e: Exception) {
            throw IllegalArgumentException("Error occurred: ${e.message}")
        }
    }
    fun makeZipFile(zipFilePath: String, tempDir: String) {
        try {
            ZipOutputStream(FileOutputStream(zipFilePath)).use { zipOut ->
                Files.walk(Paths.get(tempDir)).use { paths ->
                    paths.filter { it.toFile().isFile }.forEach { path ->
                        println("Zip file creating path $path")

                        val zipEntry = ZipEntry(Paths.get(tempDir).relativize(path).toString())
                        zipOut.putNextEntry(zipEntry)
                        Files.copy(path, zipOut)
                        zipOut.closeEntry()
                    }
                }
            }
            println("Zip file created at $zipFilePath")
        } catch (e: Exception) {
            println("An error occurred while creating ZIP file: ${e.message}")
        } finally {
         }
    }
}

data class FileXmlData(
    val contents: String,
    val keyValuePairs: Map<String, String>,
    val languageCode: String
)