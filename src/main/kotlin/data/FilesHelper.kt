package data

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


object FilesHelper {


    fun getFilesXmlContents(fileContents: Map<String, String>): Map<String, FileXmlData> {
        return fileContents.mapValues { (fileName, content) ->
            val currentPairs = parseXml(content)
            FileXmlData(
                contents = content,
                keyValuePairs = currentPairs,
                languageCode = extractLanguageCode(fileName).second
            )
        }
    }

    fun extractLanguageCode(fileName: String): Pair<String, String> {
        val regex = Regex("""values-([\w-]+)/""")
        val rawCode = regex.find(fileName)?.groups?.get(1)?.value ?: return "en" to "en"

        val standardizedCode = when (rawCode) {
            "zh", "zh-rCN" -> "zh-CN"
            "zh-rTW" -> "zh-TW"
            "in" -> "id"
            "he" -> "iw"
            "ji" -> "yi"
            else -> rawCode
        }

        return rawCode to standardizedCode
    }


    fun parseXml(xmlContent: String): Map<String, String> {
        val keyValuePairs = mutableMapOf<String, String>()
        val root = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xmlContent.toByteArray()))
            .documentElement

        val nodeList = root.getElementsByTagName("string")
        for (i in 0 until nodeList.length) {
            val stringElement = nodeList.item(i) as Element
            val key = stringElement.getAttribute("name")
            val value = stringElement.textContent
            val translatable: String = stringElement.getAttribute("translatable")
            if (key != null && value != null && translatable != "false") {
                keyValuePairs[key] = value

            }
        }
        return keyValuePairs
    }

    fun combineStringsWithLimit(
        strings: List<String>,
        separator: String,
        limit: Int = 3600
    ): List<String> {
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

    fun addNewEntriesToXmlNew(newEntries: Map<String, String>): String {
        try {
            val docFactory = DocumentBuilderFactory.newInstance()

            val docBuilder = docFactory.newDocumentBuilder()

            val doc = docBuilder.newDocument()
            val rootElement = doc.createElement("resources")
            doc.appendChild(rootElement)

            for (node in newEntries) {
                val element = doc.createElement("string")
                element.setAttribute("name", node.key)
                element.textContent = node.value
                rootElement.appendChild(element)
            }

            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")

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