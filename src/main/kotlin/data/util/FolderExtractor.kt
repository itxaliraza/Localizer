package data.util

import data.FilesHelper
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

/**
 * One translatable Android module discovered under a project root. [moduleName] is for display
 * (e.g. "app", "editor"), [resPath] is the absolute path of the module's `res` folder (the output
 * directory translations are written into), and [extraction] is the per-res-folder extraction
 * result for that module.
 */
data class ModuleExtraction(
    val moduleName: String,
    val resPath: String,
    val extraction: ExtractionResult,
    val baseStringCount: Int = 0
)

object FolderExtractor {

    // Directories that can never contain hand-authored source resources — skip them while walking
    // so we don't pick up generated `build/.../res` folders or descend into VCS/IDE metadata.
    private val EXCLUDED_DIRS = setOf("build", ".gradle", ".git", ".idea", "node_modules", "intermediates")

    /**
     * Resolves [path] to a list of translatable modules.
     *
     * - If [path] is itself a `res` folder (contains a `values/` directory) it is treated as a
     *   single module — preserving the original "point me at a res folder" behaviour.
     * - Otherwise [path] is treated as an Android project root: every `res` folder beneath it that
     *   has a `values/strings.xml` base file is discovered and returned as its own module, so a
     *   multi-module project can be translated module by module from a single root.
     */
    suspend fun extractModules(path: String): List<ModuleExtraction> = withContext(Dispatchers.IO) {
        val root = File(path.trim())
        if (!root.exists() || !root.isDirectory) return@withContext emptyList()

        findResFolders(root).map { resFolder ->
            val extraction = getKeyWithStringsFromFolder(resFolder.absolutePath)
            ModuleExtraction(
                moduleName = moduleNameOf(root, resFolder),
                resPath = resFolder.absolutePath,
                extraction = extraction,
                baseStringCount = baseStringCountOf(extraction)
            )
        }
    }

    /** Number of translatable `<string>` entries in the module's base `values/strings.xml`. */
    private fun baseStringCountOf(extraction: ExtractionResult): Int {
        val baseContent = extraction.extractedFiles["values/strings.xml"] ?: return 0
        return runCatching { FilesHelper.parseXml(baseContent).size }.getOrDefault(0)
    }

    private fun findResFolders(root: File): List<File> {
        // A res folder selected directly: keep behaving as a single-module run.
        if (File(root, "values").isDirectory) return listOf(root)

        return root.walkTopDown()
            .onEnter { it.name !in EXCLUDED_DIRS }
            .filter { it.isDirectory && it.name == "res" && File(it, "values/strings.xml").isFile }
            .toList()
    }

    /** Derives a human-readable module name from the path, e.g. `<root>/editor/src/main/res` -> "editor". */
    private fun moduleNameOf(root: File, resFolder: File): String {
        val rel = resFolder.absolutePath.removePrefix(root.absolutePath)
            .trim('\\', '/')
            .replace('\\', '/')
        return rel.substringBefore("/src").ifBlank { resFolder.parentFile?.name ?: root.name }
    }

    suspend fun getKeyWithStringsFromFolder(folderPath: String): ExtractionResult =
        withContext(Dispatchers.IO) {
            val extractedFiles = mutableMapOf<String, String>()
            val changeFileCodes = mutableMapOf<String, String>()
            val folder = File(folderPath)

            if (folder.exists() && folder.isDirectory) {
                folder.listFiles()?.sortedBy { it.name }?.forEach { valueFolder ->
                    // Only `values` and real language folders. Other qualifiers (values-night, values-v29,
                    // values-sw600dp, …) are not languages and are left completely alone.
                    if (!valueFolder.isDirectory || !FilesHelper.isLanguageFolder(valueFolder.name)) return@forEach

                    // A language folder without strings.xml still counts (its language is pre-selected),
                    // but it is modelled as an empty file in memory: loading must never write into the
                    // user's project. The file is created when a translation is actually written.
                    val stringsFile = File(valueFolder, "strings.xml")
                    val content = if (stringsFile.isFile) stringsFile.readText() else FilesHelper.EMPTY_STRINGS_XML
                    extractedFiles["${valueFolder.name}/strings.xml"] = content
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