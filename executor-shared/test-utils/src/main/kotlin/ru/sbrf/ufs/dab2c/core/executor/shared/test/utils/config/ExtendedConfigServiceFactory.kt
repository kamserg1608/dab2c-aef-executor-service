package ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.ClassPathResource
import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService
import ru.sbrf.ufs.platform.config.v3.api.ParameterStore
import ru.sbrf.ufs.platform.config.v3.factory.FileWatcherExtendedConfigService
import ru.sbrf.ufs.platform.config.v3.factory.FinderParameterSource
import ru.sbrf.ufs.platform.config.v3.file.FileWatcher
import ru.sbrf.ufs.platform.config.v3.model.Parameter
import ru.sbrf.ufs.platform.config.v3.parser.ParameterLoader
import ru.sbrf.ufs.platform.config.v3.parser.ParameterReaderFactory
import ru.sbrf.ufs.platform.config.v3.store.MapParameterStore
import java.io.IOException
import java.io.InputStreamReader
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path

object ExtendedConfigServiceFactory {

    fun buildFileBased(filepath: String): ExtendedConfigService {
        val classPathResource = ClassPathResource(filepath)
        val parameterStore: ParameterStore = MapParameterStore.build().from(readConfig(classPathResource))
        val finderParameterSource = FinderParameterSource(parameterStore)
        return FileWatcherExtendedConfigService(
            finderParameterSource,
            FileWatcher(Path.of("./"))
        )
    }

    private fun readConfig(classPathResource: ClassPathResource): Iterable<Parameter> {
        try {
            val reader = InputStreamReader(classPathResource.inputStream, StandardCharsets.UTF_8)

            val var2: Iterable<*>
            try {
                var2 = parameterLoader().load(reader)
            } catch (var5: Throwable) {
                try {
                    reader.close()
                } catch (var4: Throwable) {
                    var5.addSuppressed(var4)
                }

                throw var5
            }

            reader.close()
            return var2
        } catch (var6: IOException) {
            throw UncheckedIOException(var6)
        }
    }

    private fun parameterLoader(): ParameterLoader {
        val objectMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return ParameterLoader(objectMapper, ParameterReaderFactory.parameterReaderFactory(objectMapper))
    }
}
