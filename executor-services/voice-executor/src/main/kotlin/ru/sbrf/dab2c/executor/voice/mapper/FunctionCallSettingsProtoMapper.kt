package ru.sbrf.dab2c.executor.voice.mapper

import ru.sbrf.dab2c.executor.clients.gigavoice.proto.AudioSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.DisableInterruption
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionRanker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionSoundRule
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaChatSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.LockFunctionExecution
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Output
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.StubSounds
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Function as ProtoFunction
import ru.sbrf.dab2c.executor.domain.configuration.Settings as RestSettings

/**
 * Maps Configurator REST settings into GigaVoice protobuf Settings.
 */
object FunctionCallSettingsProtoMapper {
    /**
     * Enriches existing protobuf settings with function call settings from Configurator REST response.
     */
    fun enrich(
        protoSettings: Settings,
        restSettings: RestSettings
    ): Settings {
        val builder = protoSettings.toBuilder()

        builder.setGigachat(
            mapGigachat(restSettings)
        )

        builder.mergeAudio(
            AudioSettings.newBuilder()
                .setOutput(
                    mapOutput(restSettings)
                )
                .build()
        )

        builder.setDisableInterruption(
            mapDisableInterruption(restSettings)
        )

        return builder.build()
    }

    private fun mapGigachat(
        restSettings: RestSettings
    ): GigaChatSettings {
        val builder = GigaChatSettings.newBuilder()

        builder.addAllFunctions(
            restSettings.gigachat.functions.map { function ->
                ProtoFunction.newBuilder()
                    .setName(function.name)
                    .setDescription(function.description)
                    .setParameters(function.parameters)
                    .setReturnParameters(function.returnParameters)
                    .build()
            }
        )

        builder.setFunctionRanker(
            FunctionRanker.newBuilder()
                .addAllIgnoredFunctions(restSettings.gigachat.functionRanker.ignoredFunctions)
                .build()
        )

        return builder.build()
    }

    private fun mapOutput(
        restSettings: RestSettings
    ): Output {
        val triggerFunctionBuilder = TriggerFunction.newBuilder()

        triggerFunctionBuilder.addAllFunctionNames(
            restSettings.audio.output.stubSounds.triggerFunction.functionNames
        )

        restSettings.audio.output.stubSounds.triggerFunction.rules.forEach { rule ->
            triggerFunctionBuilder.addRules(
                FunctionSoundRule.newBuilder()
                    .addAllFunctionNames(rule.functionNames)
                    .addAllSounds(rule.sounds)
                    .build()
            )
        }

        return Output.newBuilder()
            .setStubSounds(
                StubSounds.newBuilder()
                    .setTriggerFunction(triggerFunctionBuilder.build())
                    .build()
            )
            .build()
    }

    private fun mapDisableInterruption(
        restSettings: RestSettings
    ): DisableInterruption {
        val builder = DisableInterruption.newBuilder()

        restSettings.disableInterruption.functions.forEach { function ->
            builder.addFunctions(
                LockFunctionExecution.newBuilder()
                    .setName(function.name)
                    .setOnExecution(function.onExecution)
                    .setAfterResult(function.afterResult)
                    .build()
            )
        }

        return builder.build()
    }
}
