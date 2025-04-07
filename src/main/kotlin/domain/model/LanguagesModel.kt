package domain.model

data class LanguageModel(
    val langName: String = "",
    val nativeName: String = "",
    val langCode: String = "",
    val speechCode: String = "",
    val speakCode: String = "",
    val onlyWebTranslate: Boolean = false,
)