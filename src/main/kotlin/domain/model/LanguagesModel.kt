package domain.model

data class LanguageModel(
    val langName: String,
    val nativeName: String,
    val langCode: String,
    val onlyWebTranslate: Boolean = false,
)