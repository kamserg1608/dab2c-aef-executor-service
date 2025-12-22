package ru.sbrf.ufs.dab2c.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Cookies;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = {"STUB", "stubMode", "test"})
public abstract class BasicIntegrationTest {
    private static final String REST_APP_PATH = "/v1";

    @LocalServerPort
    int port;

    @Autowired
    ServletContext servletContext;

    @Autowired
    ObjectMapper defaultObjectMapper;

    @BeforeEach
    void initRestAssured() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = servletContext.getContextPath() + REST_APP_PATH;
    }

    public Cookies createUfsStubSession() {
        return given()
                .contentType(ContentType.JSON)
                .body(loadStringResource("session/session-stub-create.json"))
            .when()
                .basePath(servletContext.getContextPath())
                .post("/session-stub/create")
            .then()
                .log().all()
                .extract()
                .detailedCookies();
    }

    public String loadStringResource(String path) {
        try {
            return StreamUtils.copyToString(getClass().getClassLoader().getResourceAsStream(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T loadJsonResource(String path, Class<T> clazz) {
        try {
            return defaultObjectMapper.readValue(getClass().getClassLoader().getResourceAsStream(path), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Заменяет плейсхолдеры в JSON-шаблоне на соответствующие значения из переданной карты.
     *
     * @param template исходная строка-шаблон запроса с плейсхолдерами
     * @param values карта, где ключ — это имя плейсхолдера, а значение — его замена
     * @return обновленная строка после подстановки всех значений
     */
    public String renderTemplate(String template, Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (template.contains(entry.getKey())) {
                if (entry.getValue() != null) {
                    template = template.replaceAll(entry.getKey(), entry.getValue());
                }
            }
        }
        return template;
    }
}
