package data.translator

import data.network.NetworkResponse
import domain.model.LanguageModel

/**
 * Translates one string value. [query] is the raw `strings.xml` value (inner XML, see
 * `FilesHelper.parseXml`); implementations must return a value in the same shape — placeholders and
 * markup intact — or a [NetworkResponse.Failure]. Exists so [TranslationManager] can be tested
 * without hitting the network.
 */
interface TranslationRepository {
    suspend fun getTranslation(
        fromLanguage: String = "en",
        toLanguage: LanguageModel,
        query: String,
    ): NetworkResponse<String>
}
