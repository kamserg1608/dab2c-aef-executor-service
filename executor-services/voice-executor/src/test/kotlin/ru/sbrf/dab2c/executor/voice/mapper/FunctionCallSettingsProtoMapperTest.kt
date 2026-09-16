package ru.sbrf.dab2c.executor.voice.mapper

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FilterSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionRegistry
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaChatSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.RequestContentSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.domain.configuration.Function
import ru.sbrf.dab2c.executor.domain.configuration.FunctionRanker
import ru.sbrf.dab2c.executor.domain.configuration.GigachatSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Function as ProtoFunction
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionRanker as ProtoFunctionRanker
import ru.sbrf.dab2c.executor.domain.configuration.Settings as RestSettings

/**
 * Verifies that Configurator settings override only the function list and the function ranker,
 * leaving the rest of the gigachat block sent by the IVR client untouched.
 */
class FunctionCallSettingsProtoMapperTest {

    @Test
    fun `should keep ivr gigachat fields and replace functions with configurator ones`() {
        val result = FunctionCallSettingsProtoMapper.enrich(ivrSettings(), restSettings(CONFIGURATOR_FUNCTIONS))

        val gigachat = result.gigachat
        assertThat(gigachat.model).isEqualTo(MODEL)
        assertThat(gigachat.temperature).isEqualTo(TEMPERATURE, within(TOLERANCE))
        assertThat(gigachat.topP).isEqualTo(TOP_P, within(TOLERANCE))
        assertThat(gigachat.currentTime).isEqualTo(CURRENT_TIME)
        assertThat(gigachat.preset).isEqualTo(PRESET)
        assertThat(gigachat.profanityCheck).isTrue()
        assertThat(gigachat.filterStubPhrasesList).containsExactly(STUB_PHRASE)
        assertThat(gigachat.filtersSettingsMap).containsOnlyKeys(FILTER_KEY)
        assertThat(gigachat.functionRegistry.profile).isEqualTo(PROFILE)
        assertThat(gigachat.functionsList.map { it.name }).containsExactly("configuratorOne", "configuratorTwo")
        assertThat(gigachat.functionsList.map { it.description }).containsExactly("first", "second")
        assertThat(gigachat.functionRanker.enabled).isTrue()
        assertThat(gigachat.functionRanker.topN).isEqualTo(RANKER_TOP_N)
        assertThat(gigachat.functionRanker.embedderModel).isEqualTo(EMBEDDER_MODEL)
        assertThat(gigachat.functionRanker.ignoredFunctionsList).containsExactly(IGNORED_FUNCTION)
    }

    @Test
    fun `should leave functions empty when configurator returned none`() {
        val result = FunctionCallSettingsProtoMapper.enrich(ivrSettings(), restSettings(emptyList()))

        val gigachat = result.gigachat
        assertThat(gigachat.functionsList).isEmpty()
        assertThat(gigachat.model).isEqualTo(MODEL)
        assertThat(gigachat.currentTime).isEqualTo(CURRENT_TIME)
        assertThat(gigachat.filterStubPhrasesList).containsExactly(STUB_PHRASE)
    }

    @Test
    fun `should apply configurator functions when ivr sent no gigachat block`() {
        val result = FunctionCallSettingsProtoMapper.enrich(
            Settings.newBuilder().setVoiceCallId(VOICE_CALL_ID).build(),
            restSettings(CONFIGURATOR_FUNCTIONS)
        )

        assertThat(result.gigachat.functionsList.map { it.name })
            .containsExactly("configuratorOne", "configuratorTwo")
    }

    private fun ivrSettings(): Settings = Settings.newBuilder()
        .setVoiceCallId(VOICE_CALL_ID)
        .setGigachat(
            GigaChatSettings.newBuilder()
                .setModel(MODEL)
                .setTemperature(TEMPERATURE)
                .setTopP(TOP_P)
                .setCurrentTime(CURRENT_TIME)
                .setPreset(PRESET)
                .setProfanityCheck(true)
                .addFilterStubPhrases(STUB_PHRASE)
                .putFiltersSettings(
                    FILTER_KEY,
                    FilterSettings.newBuilder()
                        .setRequestContent(RequestContentSettings.getDefaultInstance())
                        .build()
                )
                .setFunctionRegistry(FunctionRegistry.newBuilder().setProfile(PROFILE).build())
                .addFunctions(ProtoFunction.newBuilder().setName("ivrFunction").build())
                .setFunctionRanker(
                    ProtoFunctionRanker.newBuilder()
                        .setEnabled(true)
                        .setTopN(RANKER_TOP_N)
                        .setEmbedderModel(EMBEDDER_MODEL)
                        .addIgnoredFunctions("ivrIgnoredFunction")
                        .build()
                )
                .build()
        )
        .build()

    private fun restSettings(functions: List<Function>): RestSettings = RestSettings(
        gigachat = GigachatSettings(
            functions = functions,
            functionRanker = FunctionRanker(ignoredFunctions = listOf(IGNORED_FUNCTION))
        )
    )

    private companion object {
        const val VOICE_CALL_ID = "voice-call-id"
        const val MODEL = "GigaChat-3-Ultra-preview"
        const val TEMPERATURE = 0.7f
        const val TOP_P = 0.9f
        const val TOLERANCE = 0.0001f
        const val CURRENT_TIME = 1789395661
        const val PRESET = "preset-name"
        const val STUB_PHRASE = "секундочку"
        const val FILTER_KEY = "profanity"
        const val PROFILE = "registry-profile"
        const val IGNORED_FUNCTION = "ignoredFunction"
        const val RANKER_TOP_N = 5
        const val EMBEDDER_MODEL = "embedder-model"

        val CONFIGURATOR_FUNCTIONS = listOf(
            Function(name = "configuratorOne", description = "first", parameters = "{}", returnParameters = "{}"),
            Function(name = "configuratorTwo", description = "second", parameters = "{}", returnParameters = "{}")
        )
    }
}
