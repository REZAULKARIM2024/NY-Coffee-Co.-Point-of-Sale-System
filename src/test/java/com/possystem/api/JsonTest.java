package com.possystem.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the hand-rolled JSON reader/writer backing the REST API (no external JSON
 * library is available — Maven Central is unreachable from this project's build environment).
 */
class JsonTest {

    @Test
    void write_simpleObject_producesValidJson() {
        Map<String, Object> obj = Json.obj("name", "Latte", "price", 4.5, "active", true, "notes", null);
        String out = Json.write(obj);
        assertEquals("{\"name\":\"Latte\",\"price\":4.5,\"active\":true,\"notes\":null}", out);
    }

    @Test
    void write_escapesSpecialCharactersInStrings() {
        Map<String, Object> obj = Json.obj("text", "line1\nline2\t\"quoted\"");
        String out = Json.write(obj);
        assertEquals("{\"text\":\"line1\\nline2\\t\\\"quoted\\\"\"}", out);
    }

    @Test
    void parse_simpleObject_readsBackCorrectTypes() {
        Object parsed = Json.parse("{\"userId\": 7, \"paymentMethod\": \"CASH\", \"discount\": 1.50, \"customerId\": null}");
        assertTrue(parsed instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) parsed;
        assertEquals(7L, map.get("userId"));
        assertEquals("CASH", map.get("paymentMethod"));
        assertEquals(1.50, map.get("discount"));
        assertNull(map.get("customerId"));
    }

    @Test
    void parse_nestedArrayOfObjects() {
        Object parsed = Json.parse("{\"items\":[{\"menuItemId\":1,\"quantity\":2},{\"menuItemId\":5,\"quantity\":1}]}");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) parsed;
        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) map.get("items");
        assertEquals(2, items.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) items.get(0);
        assertEquals(1L, first.get("menuItemId"));
        assertEquals(2L, first.get("quantity"));
    }

    @Test
    void roundTrip_writeThenParse_preservesStructure() {
        Map<String, Object> original = Json.obj(
                "orderId", 42,
                "total", "12.34",
                "items", List.of(Json.obj("name", "Bagel", "qty", 3)));
        String json = Json.write(original);
        Object reparsed = Json.parse(json);
        assertTrue(reparsed instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) reparsed;
        assertEquals(42L, map.get("orderId"));
        assertEquals("12.34", map.get("total"));
    }

    @Test
    void parse_malformedJson_throwsJsonException() {
        try {
            Json.parse("{not valid json");
            assertTrue(false, "expected JsonException");
        } catch (Json.JsonException expected) {
            // pass
        }
    }
}
