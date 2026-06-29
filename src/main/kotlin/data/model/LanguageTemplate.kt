package data.model

import kotlinx.serialization.Serializable

/**
 * A user-defined, reusable bundle of target languages.
 *
 * Created from the current language selection ("Save as template") and applied later in one click.
 * Persisted as JSON by [data.util.TemplatesRepository]. [langCodes] are the `langCode`s of the
 * selected [domain.model.LanguageModel]s — codes (not full models) are stored so the file stays
 * small and resilient to the available-language list changing.
 */
@Serializable
data class LanguageTemplate(
    val id: String,
    val name: String,
    val langCodes: List<String>,
)
