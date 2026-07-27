package com.possystem.api;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal, dependency-free JSON reader/writer. The API server hand-rolls JSON instead of
 * pulling in Jackson/Gson because this project cannot reach Maven Central for extra
 * dependencies (see README) — only the JDK and vendored jars under lib/ are available.
 *
 * Supports the JSON subset this project actually needs: objects (as Map<String,Object>,
 * insertion-ordered), arrays (as List<Object>), strings, numbers (as Double or Long),
 * booleans, and null. Good enough for request/response bodies; not a spec-complete parser.
 */
public final class Json {

    private Json() {}

    // ---------- Writing ----------

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString((String) value, sb);
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long
                || value instanceof Double || value instanceof Float) {
            sb.append(value.toString());
        } else if (value instanceof BigDecimal) {
            sb.append(((BigDecimal) value).toPlainString());
        } else if (value instanceof Map) {
            writeObject((Map<String, Object>) value, sb);
        } else if (value instanceof List) {
            writeArray((List<Object>) value, sb);
        } else if (value instanceof Object[]) {
            writeArray(java.util.Arrays.asList((Object[]) value), sb);
        } else {
            writeString(value.toString(), sb);
        }
    }

    private static void writeObject(Map<String, Object> map, StringBuilder sb) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(e.getKey(), sb);
            sb.append(':');
            writeValue(e.getValue(), sb);
        }
        sb.append('}');
    }

    private static void writeArray(List<Object> list, StringBuilder sb) {
        sb.append('[');
        boolean first = true;
        for (Object o : list) {
            if (!first) sb.append(',');
            first = false;
            writeValue(o, sb);
        }
        sb.append(']');
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    /** Convenience builder for a JSON object, insertion-ordered. */
    public static Map<String, Object> obj(Object... kvPairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kvPairs.length; i += 2) {
            map.put((String) kvPairs[i], kvPairs[i + 1]);
        }
        return map;
    }

    // ---------- Parsing ----------

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWhitespace();
        Object value = p.parseValue();
        p.skipWhitespace();
        if (p.pos < p.len) throw new JsonException("Unexpected trailing content at position " + p.pos);
        return value;
    }

    public static class JsonException extends RuntimeException {
        public JsonException(String message) { super(message); }
    }

    private static class Parser {
        final String s;
        final int len;
        int pos = 0;

        Parser(String s) { this.s = s; this.len = s.length(); }

        void skipWhitespace() {
            while (pos < len && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        char peek() {
            if (pos >= len) throw new JsonException("Unexpected end of JSON input");
            return s.charAt(pos);
        }

        Object parseValue() {
            skipWhitespace();
            char c = peek();
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't':
                    expect("true"); return Boolean.TRUE;
                case 'f':
                    expect("false"); return Boolean.FALSE;
                case 'n':
                    expect("null"); return null;
                default:
                    return parseNumber();
            }
        }

        void expect(String literal) {
            if (pos + literal.length() > len || !s.startsWith(literal, pos)) {
                throw new JsonException("Expected '" + literal + "' at position " + pos);
            }
            pos += literal.length();
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // {
            skipWhitespace();
            if (peek() == '}') { pos++; return map; }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (peek() != ':') throw new JsonException("Expected ':' at position " + pos);
                pos++;
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == '}') { pos++; break; }
                throw new JsonException("Expected ',' or '}' at position " + pos);
            }
            return map;
        }

        List<Object> parseArray() {
            List<Object> list = new java.util.ArrayList<>();
            pos++; // [
            skipWhitespace();
            if (peek() == ']') { pos++; return list; }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == ']') { pos++; break; }
                throw new JsonException("Expected ',' or ']' at position " + pos);
            }
            return list;
        }

        String parseString() {
            if (peek() != '"') throw new JsonException("Expected string at position " + pos);
            pos++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= len) throw new JsonException("Unterminated string");
                char c = s.charAt(pos++);
                if (c == '"') break;
                if (c == '\\') {
                    if (pos >= len) throw new JsonException("Unterminated escape sequence");
                    char esc = s.charAt(pos++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            if (pos + 4 > len) throw new JsonException("Invalid unicode escape");
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                            break;
                        default: throw new JsonException("Invalid escape '\\" + esc + "' at position " + pos);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Object parseNumber() {
            int start = pos;
            if (peek() == '-') pos++;
            while (pos < len && Character.isDigit(s.charAt(pos))) pos++;
            boolean isDouble = false;
            if (pos < len && s.charAt(pos) == '.') {
                isDouble = true;
                pos++;
                while (pos < len && Character.isDigit(s.charAt(pos))) pos++;
            }
            if (pos < len && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                isDouble = true;
                pos++;
                if (pos < len && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
                while (pos < len && Character.isDigit(s.charAt(pos))) pos++;
            }
            String num = s.substring(start, pos);
            if (num.isEmpty() || num.equals("-")) throw new JsonException("Invalid number at position " + start);
            return isDouble ? (Object) Double.parseDouble(num) : (Object) Long.parseLong(num);
        }
    }
}
