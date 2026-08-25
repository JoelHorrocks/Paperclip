package com.joelhorrocks.paperclip.ml

class TranslationModelRemoteDataSource {
    private val models = listOf(
        RemoteTranslationModel(
            id = 1,
            name = "Generic EN-FR",
            fromLanguage = Language.EN,
            toLanguage = Language.FR,
            size = 0,
            url = null,
        ),
        RemoteTranslationModel(
            id = 2,
            name = "Generic FR-EN",
            fromLanguage = Language.FR,
            toLanguage = Language.EN,
            size = 0,
            url = null,
        )
    )

    suspend fun getModels(): List<RemoteTranslationModel> {
        return models
    }
}