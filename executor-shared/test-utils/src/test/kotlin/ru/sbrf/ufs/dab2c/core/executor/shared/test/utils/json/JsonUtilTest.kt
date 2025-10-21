package ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.json

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JsonUtilTest {

    @Test
    fun `should read JSON from file correctly`() {
        val json = JsonUtil.readJsonFromFile("json/test.json")
        assertNotNull(json)
        assertTrue(json.contains("value"))
    }

    @Test
    fun `should parse string to JsonNode correctly`() {
        val jsonStr = "{\"test\": \"Value\"}"
        val node = JsonUtil.readTree(jsonStr)
        assertEquals("Value", node["test"].asText())
    }

    @Test
    fun `should convert object to JsonNode correctly`() {
        val person = mapOf("name" to "Bob", "age" to 30)
        val node = JsonUtil.valueToTree(person)
        assertEquals("Bob", node["name"].asText())
        assertEquals(30, node["age"].asInt())
    }

    @Test
    fun `should load JSON from file as tree correctly`() {
        val node = JsonUtil.load("json/test.json")
        assertEquals("value", node["key"].asText())
    }

    @Test
    fun `should serialize object to JSON string`() {
        val person = mapOf("name" to "Bob", "age" to 25)
        val json = JsonUtil.serialize(person)
        assertTrue(json.contains("\"name\":\"Bob\""))
        assertTrue(json.contains("\"age\":25"))
    }
}
