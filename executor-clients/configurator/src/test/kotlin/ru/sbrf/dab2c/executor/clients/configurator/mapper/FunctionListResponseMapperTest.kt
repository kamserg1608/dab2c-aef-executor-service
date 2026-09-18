package ru.sbrf.dab2c.executor.clients.configurator.mapper

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.library.testing.golden.assertMatchesGolden
import ru.sbrf.dab2c.executor.clients.configurator.model.AnyExample as ApiAnyExample
import ru.sbrf.dab2c.executor.clients.configurator.model.AudioOutputSettings as ApiAudioOutputSettings
import ru.sbrf.dab2c.executor.clients.configurator.model.AudioSettings as ApiAudioSettings
import ru.sbrf.dab2c.executor.clients.configurator.model.DisableInterruptionSettings as ApiDisableInterruptionSettings
import ru.sbrf.dab2c.executor.clients.configurator.model.Function as ApiFunction
import ru.sbrf.dab2c.executor.clients.configurator.model.FunctionListResponse as ApiFunctionListResponse
import ru.sbrf.dab2c.executor.clients.configurator.model.FunctionRanker as ApiFunctionRanker
import ru.sbrf.dab2c.executor.clients.configurator.model.FunctionSoundRule as ApiFunctionSoundRule
import ru.sbrf.dab2c.executor.clients.configurator.model.GigachatSettings as ApiGigachatSettings
import ru.sbrf.dab2c.executor.clients.configurator.model.LockFunctionExecution as ApiLockFunctionExecution
import ru.sbrf.dab2c.executor.clients.configurator.model.Pair as ApiPair
import ru.sbrf.dab2c.executor.clients.configurator.model.Params as ApiParams
import ru.sbrf.dab2c.executor.clients.configurator.model.Settings as ApiSettings
import ru.sbrf.dab2c.executor.clients.configurator.model.StubSounds as ApiStubSounds
import ru.sbrf.dab2c.executor.clients.configurator.model.TriggerFunction as ApiTriggerFunction

/**
 * Mapping tests for [FunctionListResponseMapper]: whole-tree golden snapshots per response shape,
 * plus a focused check of the interruption-lock flag defaults.
 */
class FunctionListResponseMapperTest {

    @Test
    fun `maps fully populated response`() {
        val source = ApiFunctionListResponse(
            settings = ApiSettings(
                gigachat = ApiGigachatSettings(
                    functionRanker = ApiFunctionRanker(ignoredFunctions = listOf("call_operator")),
                    functions = listOf(
                        ApiFunction(
                            name = "ask_ai_expert",
                            description = "Консультация с экспертом",
                            parameters = """{"type":"object"}""",
                            fewShotExamples = listOf(
                                ApiAnyExample(
                                    request = "Где офис?",
                                    params = ApiParams(pairs = listOf(ApiPair(key = "city", value = "Москва")))
                                )
                            ),
                            returnParameters = """{"type":"object","properties":{}}"""
                        )
                    )
                ),
                audio = ApiAudioSettings(
                    output = ApiAudioOutputSettings(
                        stubSounds = ApiStubSounds(
                            triggerFunction = ApiTriggerFunction(
                                functionNames = listOf("ask_ai_expert"),
                                rules = listOf(
                                    ApiFunctionSoundRule(
                                        functionNames = listOf("ask_ai_expert"),
                                        sounds = listOf("Подождите")
                                    )
                                )
                            )
                        )
                    )
                ),
                disableInterruption = ApiDisableInterruptionSettings(
                    functions = listOf(
                        ApiLockFunctionExecution(name = "ask_ai_expert", onExecution = true, afterResult = true)
                    )
                )
            )
        )

        assertMatchesGolden(
            FunctionListResponseMapper.toDomain(source),
            "golden/FunctionListResponseMapper/full.json"
        )
    }

    @Test
    fun `defaults interruption lock flags when Configurator sends only function name`() {
        val source = ApiFunctionListResponse(
            settings = ApiSettings(
                disableInterruption = ApiDisableInterruptionSettings(
                    functions = listOf(
                        ApiLockFunctionExecution(name = "dangerous_troyka_themes"),
                        ApiLockFunctionExecution(name = "end_dialog")
                    )
                )
            )
        )

        val result = FunctionListResponseMapper.toDomain(source)

        assertThat(result.settings.disableInterruption.functions)
            .extracting("name", "onExecution", "afterResult")
            .containsExactly(
                tuple("dangerous_troyka_themes", false, false),
                tuple("end_dialog", false, false)
            )
    }

    @Test
    fun `defaults every branch when response is empty`() {
        assertMatchesGolden(
            FunctionListResponseMapper.toDomain(ApiFunctionListResponse()),
            "golden/FunctionListResponseMapper/defaults.json"
        )
    }

    @Test
    fun `defaults nested fields when intermediate objects carry no data`() {
        val source = ApiFunctionListResponse(
            settings = ApiSettings(
                gigachat = ApiGigachatSettings(
                    functionRanker = ApiFunctionRanker(),
                    functions = listOf(ApiFunction(name = "end_dialog"))
                ),
                audio = ApiAudioSettings(
                    output = ApiAudioOutputSettings(stubSounds = ApiStubSounds(triggerFunction = ApiTriggerFunction()))
                ),
                disableInterruption = ApiDisableInterruptionSettings()
            )
        )

        assertMatchesGolden(
            FunctionListResponseMapper.toDomain(source),
            "golden/FunctionListResponseMapper/partial.json"
        )
    }

    @Test
    fun `defaults nested objects when Configurator omits them`() {
        val source = ApiFunctionListResponse(
            settings = ApiSettings(
                gigachat = ApiGigachatSettings(
                    functions = listOf(ApiFunction(fewShotExamples = listOf(ApiAnyExample())))
                ),
                audio = ApiAudioSettings()
            )
        )

        assertMatchesGolden(
            FunctionListResponseMapper.toDomain(source),
            "golden/FunctionListResponseMapper/nested-nulls.json"
        )
    }
}
