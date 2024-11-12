package data.translator.api_interface

import data.network.NetworkResponse

interface TranslatorApis {
    suspend fun getTranslation(
        fromLanguage: String?,
        toLanguage: String?,
        query: String?,
    ): NetworkResponse<String>
}