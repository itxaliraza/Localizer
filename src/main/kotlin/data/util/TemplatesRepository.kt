package data.util

import data.model.LanguageTemplate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists [LanguageTemplate]s to a small JSON file in the user's home directory
 * (`~/.fast-localizer/templates.json`), so saved language sets survive app restarts.
 *
 * This replaces the old import/export-to-Downloads flow: templates live inside the app and are
 * loaded once at startup. All methods are crash-safe — a missing or corrupt file yields an empty
 * list rather than throwing, since templates are convenience data, never critical.
 */
class TemplatesRepository {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val file: File by lazy {
        val dir = File(System.getProperty("user.home"), ".fast-localizer")
        if (!dir.exists()) dir.mkdirs()
        File(dir, "templates.json")
    }

    fun load(): List<LanguageTemplate> {
        return try {
            if (!file.exists()) return emptyList()
            val text = file.readText()
            if (text.isBlank()) return emptyList()
            val stored: List<LanguageTemplate> = json.decodeFromString(text)
            val migrated = stored.map(::withCurrentCodes)
            if (migrated != stored) save(migrated)
            migrated
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Rewrites old spellings of a code to the one the app uses now (`id` -> `in`, `itg` -> `ltg`,
     * `pt` -> `pt-PT`), so templates saved by earlier versions keep their languages and are still
     * highlighted as Active. Codes that match no supported language are kept as they are.
     */
    private fun withCurrentCodes(template: LanguageTemplate): LanguageTemplate =
        template.copy(
            langCodes = template.langCodes.map { LanguageCodeResolver.resolve(it)?.langCode ?: it }.distinct()
        )

    fun save(templates: List<LanguageTemplate>) {
        try {
            file.writeText(json.encodeToString(templates))
        } catch (e: Exception) {
            // Non-critical: failing to persist a template should never crash the app.
        }
    }
}
