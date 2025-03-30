package data

import domain.model.LanguageModel


val availableLanguagesList: List<LanguageModel> by lazy {
    listOf(
        LanguageModel("Afrikaans", "af"),
        LanguageModel("Albanian", "sq"),
        LanguageModel("Amharic", "am"),
        LanguageModel("Arabic", "ar"),
        LanguageModel("Armenian", "hy"),
        LanguageModel("Azerbaijani", "az"),
        LanguageModel("Basque", "eu"),
        LanguageModel("Belarusian", "be"),
        LanguageModel("Bengali", "bn"),
        LanguageModel("Bosnian", "bs"),
        LanguageModel("Bulgarian", "bg"),
        LanguageModel("Catalan", "ca"),
        LanguageModel("Cebuano", "ceb"),
        LanguageModel("Chinese (Simplified)", "zh-CN"),
        LanguageModel("Chinese (Traditional)", "zh-TW"),
        LanguageModel("Corsican", "co"),
        LanguageModel("Croatian", "hr"),
        LanguageModel("Czech", "cs"),
        LanguageModel("Danish", "da"),
        LanguageModel("Dutch", "nl"),
        LanguageModel("Esperanto", "eo"),
        LanguageModel("Estonian", "et"),
        LanguageModel("Finnish", "fi"),
        LanguageModel("French", "fr"),
        LanguageModel("Frisian", "fy"),
        LanguageModel("Galician", "gl"),
        LanguageModel("Georgian", "ka"),
        LanguageModel("German", "de"),
        LanguageModel("Greek", "el"),
        LanguageModel("Gujarati", "gu"),
        LanguageModel("Haitian Creole", "ht"),
        LanguageModel("Hausa", "ha"),
        LanguageModel("Hawaiian", "haw"),
        LanguageModel("Hebrew he", "he"),
        LanguageModel("Hebrew iw", "iw"),
        LanguageModel("Hindi", "hi"),
        LanguageModel("Hmong", "hmn"),
        LanguageModel("Hungarian", "hu"),
        LanguageModel("Icelandic", "is"),
        LanguageModel("Igbo", "ig"),
        LanguageModel("Indonesian id", "id"),
        LanguageModel("Indonesian In", "in"),
        LanguageModel("Irish", "ga"),
        LanguageModel("Italian", "it"),
        LanguageModel("Japanese", "ja"),
        LanguageModel("Javanese", "jw"),
        LanguageModel("Kannada", "kn"),
        LanguageModel("Kazakh", "kk"),
        LanguageModel("Khmer", "km"),
        LanguageModel("Kinyarwanda", "rw"),
        LanguageModel("Korean", "ko"),
        LanguageModel("Kurdish", "ku"),
        LanguageModel("Kyrgyz", "ky"),
        LanguageModel("Lao", "lo"),
        LanguageModel("Latin", "la"),
        LanguageModel("Latvian", "lv"),
        LanguageModel("Lithuanian", "lt"),
        LanguageModel("Luxembourgish", "lb"),
        LanguageModel("Macedonian", "mk"),
        LanguageModel("Malagasy", "mg"),
        LanguageModel("Malay", "ms"),
        LanguageModel("Malayalam", "ml"),
        LanguageModel("Maltese", "mt"),
        LanguageModel("Maori", "mi"),
        LanguageModel("Marathi", "mr"),
        LanguageModel("Mongolian", "mn"),
        LanguageModel("Myanmar (Burmese)", "my"),
        LanguageModel("Nepali", "ne"),
        LanguageModel("Norwegian", "no"),
        LanguageModel("Nyanja (Chichewa)", "ny"),
        LanguageModel("Odia (Oriya)", "or"),
        LanguageModel("Oromo", "om"),
        LanguageModel("Pashto", "ps"),
        LanguageModel("Persian", "fa"),
        LanguageModel("Polish", "pl"),
        LanguageModel("Portuguese", "pt"),
        LanguageModel("Punjabi", "pa"),
        LanguageModel("Romanian", "ro"),
        LanguageModel("Russian", "ru"),
        LanguageModel("Samoan", "sm"),
        LanguageModel("Scots Gaelic", "gd"),
        LanguageModel("Serbian", "sr"),
        LanguageModel("Sesotho", "st"),
        LanguageModel("Shona", "sn"),
        LanguageModel("Sindhi", "sd"),
        LanguageModel("Sinhala (Sinhalese)", "si"),
        LanguageModel("Slovak", "sk"),
        LanguageModel("Slovenian", "sl"),
        LanguageModel("Somali", "so"),
        LanguageModel("Spanish", "es"),
        LanguageModel("Sundanese", "su"),
        LanguageModel("Swahili", "sw"),
        LanguageModel("Swedish", "sv"),
        LanguageModel("Tagalog (Filipino)", "tl"),
        LanguageModel("Tajik", "tg"),
        LanguageModel("Tamil", "ta"),
        LanguageModel("Tatar", "tt"),
        LanguageModel("Telugu", "te"),
        LanguageModel("Thai", "th"),
        LanguageModel("Turkish", "tr"),
        LanguageModel("Turkmen", "tk"),
        LanguageModel("Ukrainian", "uk"),
        LanguageModel("Urdu", "ur"),
        LanguageModel("Uyghur", "ug"),
        LanguageModel("Uzbek", "uz"),
        LanguageModel("Vietnamese", "vi"),
        LanguageModel("Welsh", "cy"),
        LanguageModel("Xhosa", "xh"),
        LanguageModel("Yiddish yi", "yi"),
        LanguageModel("Yiddish ji", "ji"),
        LanguageModel("Yoruba", "yo"),
        LanguageModel("Zulu", "zu")
    )
}


fun availableLanguages(): ArrayList<LanguageModel> {
    return ArrayList<LanguageModel>().apply {
        add(
            LanguageModel(
                "English", "English", "en",
                "en-US",
                "en-US",
            )
        )
        add(
            LanguageModel(
                "Abkhaz",
                "Аԥсуа бызшәа (Apsua byzshwa)",
                "ab",
                "",
                "",
                true,

                )
        )
        add(LanguageModel("Acehnese", "Bahasa Acèh", "ace", "", "", true))
        add(LanguageModel("Acholi", "Leb Acoli", "ach", "", "", true))
        add(LanguageModel("Afar", "Qafár af", "aa", "", "", true))
        add(
            LanguageModel(
                "Afrikaans",
                "Afrikaans",
                "af",
                "af-ZA",
                "af-ZA",

                )
        )
        add(
            LanguageModel(
                "Albanian",
                "Shqiptar",
                "sq",
                "",
                "sq",

                )
        )
        add(LanguageModel("Alur", "Dho Alur", "alz", "", "", true))
        add(
            LanguageModel(
                "Amharic",
                "አማርኛ",
                "am",
                "am-ET",
                "",

                )
        )
        add(
            LanguageModel(
                "Arabic",
                "العربية",
                "ar",
                "ar-SA",
                "ar-SA",

                )
        )
        add(
            LanguageModel(
                "Armenian",
                "Հայերեն",
                "hy",
                "hy-AM",
                "",

                )
        )
        add(
            LanguageModel(
                "Assamese",
                "অসমীয়া",
                "as",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Avar",
                "МагӀарул мацӀ (Magarul macʼ)",
                "av",
                "",
                "",
                true,

                )
        )
        add(LanguageModel("Awadhi", "अवधी ", "awa", "", "", true))
        add(LanguageModel("Aymara", "Aymara", "ay", "", ""))
        add(LanguageModel("Azerbaijani", "Azərbaycan", "az", "az-AZ", "", true))
        add(LanguageModel("Balinese", "ᬩᬲᬩᬮᬶ ", "ban", "", "", true))
        add(LanguageModel("Baluchi", "بلۏچی", "bal", "", "", true))
        add(LanguageModel("Bambara", "ߓߡߊߣߊ߲", "bm", "", ""))
        add(LanguageModel("Baoulé", "Bawule", "bci", "", "", true))
        add(LanguageModel("Bashkir", "Башҡорт теле", "ba", "", "", true))
        add(
            LanguageModel(
                "Basque",
                "Euskara",
                "eu",
                "eu-ES",
                "",

                )
        )
        add(LanguageModel("Batak Karo", "Hata Karo", "btx", "", "", true))
        add(
            LanguageModel(
                "Batak Simalungun",
                "Hata Simalungun",
                "bts",
                "",
                "",
                true,
            )
        )
        add(LanguageModel("Batak Toba", "Hata Batak Toba", "bbc", "", "", true))
        add(
            LanguageModel(
                "Belarusian",
                "Беларуская",
                "be",
                "",
                "",

                )
        )
        add(LanguageModel("Bemba", "ChiBemba", "bem", "", "", true))
        add(
            LanguageModel(
                "Bengali",
                "বাংলা",
                "bn",
                "bn-BD",
                "bn-BD",

                )
        )
        add(LanguageModel("Betawi", "Baso Betaw", "bew", "", "", true))
        add(
            LanguageModel(
                "Bhojpuri",
                "भोजपुरीया",
                "bho",
                "",
                "",

                )
        )
        add(LanguageModel("Bikol", "Bikol", "bik", "", "", true))
        add(
            LanguageModel(
                "Bosnian",
                "Bosanski",
                "bs",
                "",
                "bs",

                )
        )
        add(LanguageModel("Breton", "Brezhoneg", "bs", "", "", true))
        add(
            LanguageModel(
                "Bulgarian",
                "български",
                "bg",
                "bg-BG",
                "",

                )
        )
        add(LanguageModel("Buryat", "Буряад хэлэн", "bua", "", "", true))
        add(LanguageModel("Cantonese", "廣東話 ", "yue", "zh-HK", "zh-HK", true))
        add(
            LanguageModel(
                "Catalan",
                "Català",
                "ca",
                "ca-ES",
                "ca-ES",

                )
        )

        add(LanguageModel("Cebuano", "Cebuano", "ceb", "", ""))
        add(LanguageModel("Chamorro", "Fino' Chamoru", "ch", "", "", true))
        add(LanguageModel("Chechen", "Нохчийн мотт", "ce", "", "", true))
        add(
            LanguageModel(
                "Chichewa",
                "Chichewa",
                "ny",
                "ny",
                "",

                )
        )
        add(
            LanguageModel(
                "Chinese (Simplified)",
                "简体中文",
                "zh-CN",
                "cmn-Hans-CN",
                "cmn-Hans-CN",
                )
        )
        add(
            LanguageModel(
                "Chinese (Traditional)",
                "简体中文",
                "zh-TW",
                "cmn-Hans-TW",
                "cmn-Hans-TW",
                )
        )
        add(LanguageModel("Chuukese", "Chuuk", "chk", "", "", true))
        add(LanguageModel("Chuvash", "Чӑваш чӗлхи", "cv", "", "", true))
        add(
            LanguageModel(
                "Corsican",
                "Corsu",
                "co",
                "hr-HR",
                "",

                )
        )
        add(
            LanguageModel(
                "Crimean Tatar",
                "Qırımtatar tili",
                "crh",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Croatian",
                "Hrvatski",
                "hr",
                "hr-HR",
                "hr",

                )
        )
        add(
            LanguageModel(
                "Czech",
                "Čeština",
                "cs",
                "cs-CZ",
                "cs-CZ",

                )
        )
        add(
            LanguageModel(
                "Danish",
                "Dansk",
                "da",
                "da-DK",
                "da-DK",

                )
        )
        add(
            LanguageModel(
                "Dari",
                "دری",
                "fa-AF",
                "fa-AF",
                "",
                true,

                )
        )
        add(LanguageModel("Dhivehi", "ދިވެހި", "dv", "", ""))
        add(LanguageModel("Dinka", "Thuɔŋjäŋ", "din", "", "", true))
        add(LanguageModel("Dogri", "डोगरा", "doi", "", ""))
        add(LanguageModel("Dombe", "Dombe", "dov", "", "", true))
        add(
            LanguageModel(
                "Dutch",
                "Nederlands",
                "nl",
                "nl-NL",
                "nl-NL",

                )
        )
        add(LanguageModel("Dyula", "Julakan", "dyu", "", "", true))
        add(LanguageModel("Dzongkha", "རྫོང་ཁ (Dzongkha)", "dz", "", "", true))
        add(
            LanguageModel(
                "Esperanto",
                "Esperanto",
                "eo",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Estonian",
                "Eesti",
                "et",
                "et-EE",
                "et",

                )
        )
        add(LanguageModel("Ewe", "Eʋeawo", "ee", "", ""))
        add(LanguageModel("Faroese", "Føroyskt", "fo", "", "", true))
        add(LanguageModel("Fijian", "Vosa Vakaviti", "fj", "", "", true))
        add(
            LanguageModel(
                "Filipino",
                "Pilipino",
                "tl",
                "fil-PH",
                "tl",

                )
        )
        add(
            LanguageModel(
                "Finnish",
                "Suomi",
                "fi",
                "fi-FI",
                "fi-FI",

                )
        )
        add(LanguageModel("Fon", "Fɔngbè", "fon", "", "", true))
        add(
            LanguageModel(
                "French",
                "Français",
                "fr",
                "fr-FR",
                "fr-FR",

                )
        )
        add(
            LanguageModel(
                "Frisian", "Frysk",
                "fy", "fy", "",

                )
        )


        add(LanguageModel("Friulian", "furlan", "fur", "", "", true))
        add(LanguageModel("Fulani", "Pulaar", "ff", "", "", true))
        add(LanguageModel("Ga", "Gã", "gaa", "", "", true))
        add(
            LanguageModel(
                "Galician",
                "Galego",
                "gl",
                "gl-ES",
                "",

                )
        )
        add(
            LanguageModel(
                "Georgian",
                "ქართული",
                "ka",
                "ka-GE",
                "",

                )
        )
        add(
            LanguageModel(
                "German",
                "Deutsch",
                "de",
                "de-DE",
                "de-DE",

                )
        )
        add(
            LanguageModel(
                "Greek",
                "Ελληνικά",
                "el",
                "el-GR",
                "el-GR",

                )
        )
        add(LanguageModel("Guarani", "Guarani", "gn", "", ""))
        add(
            LanguageModel(
                "Gujarati",
                "ગુજરાતી",
                "gu",
                "gu-IN",
                "gu",

                )
        )
        add(
            LanguageModel(
                "Haitian Creole",
                "Kreyòl Ayisyen",
                "ht",
                "",
                "",

                )
        )
        add(LanguageModel("Hakha Chin", "Lai Holh", "cnh", "", "", true))
        add(
            LanguageModel(
                "Hausa",
                "Hausa",
                "ha",
                "ha",
                "ha",

                )
        )
        add(
            LanguageModel(
                "Hawaiian",
                "Hawaiian",
                "haw",
                "iw",
                "",

                )
        )
        add(
            LanguageModel(
                "Hebrew",
                "עִברִית",
                "iw",
                "he-IL",
                "iw",

                )
        )
        add(LanguageModel("Hiligaynon", "Hiligaynon", "hil", "", "", true))
        add(
            LanguageModel(
                "Hindi",
                "हिन्दी",
                "hi",
                "hi-IN",
                "hi-IN",

                )
        )
        add(
            LanguageModel(
                "Hmong", "Hmong",
                "hmn", "", "",

                )
        )


        add(
            LanguageModel(
                "Hungarian",
                "Magyar",
                "hu",
                "hu-HU",
                "hu-HU",

                )
        )
        add(LanguageModel("Hunsrik", "Hunsrik", "hrx", "", "", true))
        add(LanguageModel("Iban", "Jaku Ibanc", "iba", "", "", true))
        add(
            LanguageModel(
                "Icelandic",
                "Íslenska",
                "is",
                "is-IS",
                "is-IS",

                )
        )
        add(
            LanguageModel(
                "Igbo", "Igbo",
                "ig", "", "",

                )
        )


        add(LanguageModel("Ilocano", "Ilokano", "ilo", "", ""))
        add(
            LanguageModel(
                "Indonesian",
                "Bahasa Indonesia",
                "in",
                "id-ID",
                "id-ID",

                )
        )
        add(
            LanguageModel(
                "Irish",
                "Gaeilge",
                "ga",
                "ga",
                "ga",

                )
        )
        add(
            LanguageModel(
                "Italian",
                "Italiano",
                "it",
                "it-IT",
                "it-IT",

                )
        )
        add(LanguageModel("Jamaican Patois", "Patwa", "jam", "", "", true))
        add(
            LanguageModel(
                "Japanese",
                "日本語",
                "ja",
                "ja-JP",
                "ja-JP",

                )
        )
        add(
            LanguageModel(
                "Javanese",
                "Jawa",
                "jw",
                "jv-ID",
                "jw",

                )
        )
        add(LanguageModel("Jingpo", "Jingpho", "kac", "", "", true))
        add(LanguageModel("Kalaallisut", "Kalaallisut", "kl", "", "", true))
        add(
            LanguageModel(
                "Kannada",
                "ಕನ್ನಡ",
                "kn",
                "kn-IN",
                "kn",

                )
        )
        add(LanguageModel("Kanuri", "Kanuri", "kr", "", "", true))
        add(
            LanguageModel(
                "Kapampangan",
                "Amánung Kapampángan",
                "pam",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Kazakh", "Қазақ",
                "kk", "", "",

                )
        )
        add(
            LanguageModel(
                "Khasi",
                "Ka Ktien Khasi",
                "kha",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Khmer", "ភាសាខ្មែរ",
                "km", "km-KH", "km-KH",
                true,
            )
        )



        add(
            LanguageModel(
                "Kiga", "Rukiga",
                "cgg", "", "",
                true,
            )
        )



        add(LanguageModel("Kikongo", "Kikongo", "kg", "", "", true))
        add(
            LanguageModel(
                "Kinyarwanda",
                "Kinyarwanda",
                "rw",
                "",
                "",

                )
        )
        add(LanguageModel("Kituba", "Kituba", "ktu", "", "", true))
        add(LanguageModel("Kokborok", "𑄇𑄧𑄇𑄴𑄝𑄧𑄢𑄧𑄇𑄴", "trp", "", "", true))
        add(LanguageModel("Komi", "коми кыв (komi kyv)", "kv", "", "", true))
        add(
            LanguageModel(
                "Konkani",
                "कोंकणी ",
                "gom",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Korean",
                "한국어",
                "ko",
                "ko-KR",
                "ko-KR",

                )
        )
        add(LanguageModel("Krio", "Krio", "kri", "", ""))
        add(
            LanguageModel(
                "Kurdish (Sorani)",
                "کوردی",
                "ckb",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Kurdish(Kurmanji)",
                "Kurdî (Kurmancî)",
                "ku",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Kyrgyz", "Кыргызча",
                "ky", "", "",

                )
        )


        add(
            LanguageModel(
                "Lao", "ລາວ",
                "lo", "lo-LA",
                "",
            )
        )



        add(LanguageModel("Latgalian", "Latgalīšu volūda", "itg", "", "", true))

        add(
            LanguageModel(
                "Latin", "Latina",
                "la", "", "la",

                )
        )

        add(
            LanguageModel(
                "Latvian",
                "Latviešu",
                "lv",
                "lv-LV",
                "lv-LV",

                )
        )
        add(LanguageModel("Ligurian", "Lìgure", "lij", "", "", true))
        add(LanguageModel("Limburgish", "Limburgs", "li", "", "", true))
        add(LanguageModel("Lingala", "Lingala", "ln", "", ""))
        add(
            LanguageModel(
                "Lithuanian",
                "Lietuvių",
                "lt",
                "lt-LT",
                "",

                )
        )
        add(LanguageModel("Lombard", "Lumbaart", "lmo", "", "", true))
        add(LanguageModel("Luganda", "Oluganda", "lg", "", ""))
        add(LanguageModel("Luo", "Dholuo", "luo", "", "", true))
        add(
            LanguageModel(
                "Luxembourgish",
                "Lëtzebuergesch",
                "lb",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Macedonian",
                "Македонски",
                "mk",
                "",
                "",

                )
        )
        add(LanguageModel("Madurese", "Madhurâ", "mad", "", "", true))
        add(
            LanguageModel(
                "Maithili",
                "मैथिली ",
                "mai",
                "",
                "",

                )
        )
        add(LanguageModel("Makassar", "ᨅᨔ ᨕᨘᨁᨗ", "mak", "", "", true))
        add(
            LanguageModel(
                "Malagasy",
                "Malagasy",
                "mg",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Malay",
                "Melayu",
                "ms",
                "ms-MY",
                "",

                )
        )
        add(
            LanguageModel(
                "Malay (Jawi)",
                "بهاس ملايو",
                "ms-Arab",
                "ms-Arab",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Malayalam",
                "മലയാളം",
                "ml",
                "ml-IN",
                "ml",

                )
        )
        add(
            LanguageModel(
                "Maltese", "Malti", "mt",
                "", "",
            )
        )


        add(LanguageModel("Mam", "Qyol Mam", "mam", "", "", true))
        add(LanguageModel("Manx", "Gaelg", "gv", "", "", true))
        add(
            LanguageModel(
                "Maori", "Māori",
                "mi", "", "",

                )
        )



        add(
            LanguageModel(
                "Marathi",
                "मराठी",
                "mr",
                "mr-IN",
                "mr",

                )
        )
        add(LanguageModel("Marshallese", "Kajin M̧ajeļ", "mh", "", "", true))
        add(LanguageModel("Marwadi", "मारवाड़ी ", "mwr", "", "", true))
        add(
            LanguageModel(
                "Mauritian Creole",
                "Kreol Morisien",
                "mfe",
                "",
                "",
                true,

                )
        )
        add(LanguageModel("Meadow Mari", "марий йылме", "chm", "", "", true))
        add(
            LanguageModel(
                "Meiteilon Manipuri",
                "ꯃꯩꯇꯩꯂꯣꯟ ",
                "mni-Mtei",
                "",
                "",

                )
        )
        add(LanguageModel("Minang", "Baso Minang", "min", "", "", true))
        add(
            LanguageModel(
                "Mizo",
                "Mizo ṭawng",
                "lus",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Mongolian",
                "Монгол",
                "mn",
                "mn",
                "",

                )
        )
        add(
            LanguageModel(
                "Myanmar(Burmese)",
                "မြန်မာ (ဗမာ)",
                "my",
                "",
                "my",

                )
        )
        add(LanguageModel("NKo", "ߒߞߏ", "bm-Nkoo", "bm-Nkoo", "", true))
        add(
            LanguageModel(
                "Nahuatl (Eastern Huasteca)",
                "Macehualtlahtolli",
                "nhe",
                "",
                "",
                true,
            )
        )
        add(LanguageModel("Ndau", "chiNdau", "ndc-ZW", "ndc-ZW", "", true))
        add(LanguageModel("Ndebele (South)", "नेपालभाषा ", "nr", "", "", true))
        add(
            LanguageModel(
                "Nepalbhasa (Newari)",
                "नेपालभाषा ",
                "new",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Nepali",
                "नेपाली",
                "ne",
                "ne-NP",
                "ne-NP",

                )
        )
        add(
            LanguageModel(
                "Norwegian",
                "Norsk",
                "no",
                "nb-NO",
                "nb-NO",

                )
        )
        add(LanguageModel("Nuer", "Thok Nath", "nus", "", "", true))
        add(LanguageModel("Occitan", "Occitan", "oc", "", "", true))
        add(
            LanguageModel(
                "Odia (Oriya)",
                "ଓଡ଼ିଆ",
                "or",
                "",
                "",

                )
        )
        add(LanguageModel("Oromo", "Afaan Oromoo", "om", "", "", true))
        add(
            LanguageModel(
                "Ossetian",
                "ирон ӕвзаг (Iron ævzag)",
                "os",
                "",
                "",
                true,

                )
        )
        add(LanguageModel("Pangasinan", "Pangasinan", "pag", "", "", true))
        add(LanguageModel("Papiamento", "Papiamentu", "pap", "", "", true))

        add(
            LanguageModel(
                "Pashto", "پښتو",
                "ps", "", "",

                )
        )

        add(
            LanguageModel(
                "Persian",
                "فارسی",
                "fa",
                "fa-IR",
                "",

                )
        )
        add(
            LanguageModel(
                "Polish",
                "Polski",
                "pl",
                "pl-PL",
                "pl-PL",

                )
        )
        add(
            LanguageModel(
                "Portuguese (Brazil)",
                "Português ",
                "pt-BR",
                "pt-BR",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Portuguese (Portugal)",
                "Português ",
                "pt-PT",
                "pt-PT",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Punjabi (Gurmukhi)",
                "ਪੰਜਾਬੀ",
                "pa",
                "pa",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Punjabi (Shahmukhi)",
                "پنجابی",
                "pa-Arab",
                "pa-Arab",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Quechua",
                "Runa Simi",
                "qu",
                "",
                "",

                )
        )
        add(LanguageModel("Qʼeqchiʼ", "Q’eqchi’", "kek", "", "", true))
        add(LanguageModel("Romani", "Romani", "rom", "", "", true))
        add(
            LanguageModel(
                "Romanian",
                "Română",
                "ro",
                "ro-RO",
                "ro-RO",

                )
        )
        add(LanguageModel("Rundi", "Ikirundi", "rn", "", "", true))
        add(
            LanguageModel(
                "Russian",
                "Русский",
                "ru",
                "ru-RU",
                "ru-RU",

                )
        )
        add(LanguageModel("Sami (North)", "Davvisámegiella", "se", "", "", true))

        add(
            LanguageModel(
                "Samoan", "Samoa",
                "sm", "", "",

                )
        )


        add(LanguageModel("Sango", "Sängö", "sg", "", "", true))
        add(
            LanguageModel(
                "Sanskrit",
                "संस्कृतम् ",
                "sa",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Santali",
                "ᱥᱟᱱᱛᱟᱲᱤ",
                "sat-Latn",
                "sat-Latn",
                "",
                true,

                )
        )


///         TEST BELOW LANGUAGES BEFORE RELEASE, MAY CONTAIN ANY KIND OF ISSUE
        add(
            LanguageModel(
                "Scots Gaeli",
                "Gàidhlig na h-Alba",
                "gd",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Sepedi",
                "Sesotho sa Leboa",
                "nso",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Serbian",
                "Српски",
                "sr",
                "sr-RS",
                "sr-RS",

                )
        )
        add(
            LanguageModel(
                "Sesotho", "Sesotho",
                "st", "", "",

                )
        )


        add(
            LanguageModel(
                "Seychellois Creole",
                "Kreol Seselwa",
                "crs",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Shan",
                "ၵႂၢမ်းတႆ",
                "shn",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Shona", "Shona",
                "sn", "", "",

                )
        )


        add(
            LanguageModel(
                "Sicilian",
                "Sicilianu",
                "scn",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Silesian",
                "Ślōnskŏ gŏdka",
                "szl",
                "",
                "",
                true,

                )
        )
        add(LanguageModel("Sindhi", "سنڌي", "sd", "", ""))
        add(
            LanguageModel(
                "Sinhala",
                "සිංහල",
                "si",
                "si-LK",
                "si-LK",

                )
        )
        add(
            LanguageModel(
                "Slovak",
                "Slovenčina",
                "sk",
                "sk-SK",
                "sk-SK",

                )
        )
        add(
            LanguageModel(
                "Slovenian",
                "Slovenščina",
                "sl",
                "sl-SI",
                "",

                )
        )
        add(
            LanguageModel(
                "Somali", "Soomaali",
                "so", "", "",

                )
        )

        add(
            LanguageModel(
                "Spanish",
                "Español",
                "es",
                "es-ES",
                "es-ES",

                )
        )
        add(
            LanguageModel(
                "Sundanese",
                "Basa Sunda",
                "su",
                "su-ID",
                "su",

                )
        )
        add(LanguageModel("Susu", "Sosoxui", "sus", "", "", true))
        add(
            LanguageModel(
                "Swahili",
                "Kiswahili",
                "sw",
                "sw-TZ",
                "sw-TZ",

                )
        )
        add(
            LanguageModel(
                "Swati",
                "SiSwati",
                "ss",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Swedish", "Svenska",
                "sv", "sv-SE", "sv-SE",
                true,
            )
        )
        add(
            LanguageModel(
                "Tahitian",
                "Reo Tahiti",
                "ty",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Tajik", "Тоҷикӣ",
                "tg", "", "",

                )
        )

        add(
            LanguageModel(
                "Tamazight",
                "Tamaziɣt",
                "ber-Latn",
                "ber-Latn",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Tamazight (Tifinagh)",
                "ⵜⴰⵎⴰⵣⵉⵖⵜ (Tamaziɣt)",
                "ber",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Tamil",
                "தமிழ்",
                "ta",
                "ta-IN",
                "ta-IN",

                )
        )
        /// FLAG OF TATAR LANGUAGE IS NOT CORRECT
        add(
            LanguageModel(
                "Tatar", "Tatarça",
                "tt", "", "",

                )
        )

        add(
            LanguageModel(
                "Telugu",
                "తెలుగు",
                "te",
                "te-IN",
                "te",

                )
        )
        add(LanguageModel("Tetum", "Tetun", "tet", "", "", true))
        add(
            LanguageModel(
                "Thai",
                "ไทย",
                "th",
                "th-TH",
                "th-TH",

                )
        )
        add(
            LanguageModel(
                "Tibetan",
                "བོད་སྐད།",
                "bo",
                "",
                "",
                true,

                )
        )
        add(LanguageModel("Tigrinya", "ትግርኛ ", "ti", "", ""))
        add(LanguageModel("Tiv", "Tiv", "tiv", "", "", true))
        add(LanguageModel("Tok Pisin", "Tok Pisin", "tpi", "", "", true))
        add(LanguageModel("Tongan", "Lea faka-Tonga", "to", "", "", true))
        add(
            LanguageModel(
                "Tsonga",
                "Xitsonga",
                "ts",
                "",
                "ts",

                )
        )
        add(LanguageModel("Tswana", "Setswana", "tn", "", "", true))
        add(LanguageModel("Tulu", "ತುಳು", "tcy", "", "", true))
        add(LanguageModel("Tumbuka", "Chitumbuka", "tum", "", "", true))
        add(
            LanguageModel(
                "Turkish",
                "Türkçe",
                "tr",
                "tr-TR",
                "tr-TR",

                )
        )
        add(
            LanguageModel(
                "Turkmen",
                "Түркмен", "tk",
                "", "",
                true,
            )
        )


        add(LanguageModel("Tuvan", "Тыва дыл (Tyva dyl)", "tyv", "", "", true))
        add(LanguageModel("Twi", "Twi", "ak", "", ""))
        add(
            LanguageModel(
                "Udmurt",
                "Удмурт кыл (Udmurt kyl)",
                "udm",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Ukrainian",
                "Українська",
                "uk",
                "uk-UA",
                "uk",

                )
        )
        add(
            LanguageModel(
                "Urdu", "اردو",
                "ur", "ur-PK", "ur",

                )
        )



        add(
            LanguageModel(
                "Uyghur", "ۇيغۇر",
                "ug", "", "",

                )
        )


        add(
            LanguageModel(
                "Uzbek", "O'zbek",
                "uz", "", "",

                )
        )
        add(LanguageModel("Venda", "Tshivenda", "ve", "", "", true))
        add(LanguageModel("Venetian", "Vèneto", "vec", "", "", true))
        add(
            LanguageModel(
                "Vietnamese",
                "Tiếng Việt",
                "vi",
                "vi-VN",
                "vi-VN",

                )
        )
        add(LanguageModel("Waray", "Winaray", "war", "", "", true))
        add(
            LanguageModel(
                "Welsh",
                "Cymraeg",
                "cy",
                "",
                "",

                )
        )
        add(LanguageModel("Wolof", "Wolof", "cy", "wo", "", true))
        add(LanguageModel("Xhosa", "isiXhosa", "xh", "", ""))
        add(
            LanguageModel(
                "Yakut",
                "Саха тыла (Sakha tıla)",
                "sah",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Yiddish", "ייִדיש",
                "yi", "", "",

                )
        )
        add(
            LanguageModel(
                "Yoruba",
                "Yorùbá",
                "yo",
                "",
                "",

                )
        )
        add(
            LanguageModel(
                "Yucatec Maya",
                "Màaya t'àan",
                "yua",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Zapotec",
                "Diidxazá",
                "zap",
                "",
                "",
                true,

                )
        )
        add(
            LanguageModel(
                "Zulu",
                "isiZulu",
                "zu",
                "zu-ZA",
                "",

                )
        )

    }
}