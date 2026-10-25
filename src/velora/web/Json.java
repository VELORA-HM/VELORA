package velora.web;

import java.util.HashMap;
import java.util.Map;

/** Minimal JSON helpers so the server needs no extra library. Handles flat request objects only. */
public final class Json {
    private Json() {}

    public static String str(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    public static String num(double d) {
        return String.format(java.util.Locale.ROOT, "%.2f", d);
    }

    /** Parses {"key": "value", "n": 2, "flag": true} into a map of strings. */
    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new HashMap<>();
        int[] i = {skip(json, 0)};
        if (i[0] >= json.length() || json.charAt(i[0]) != '{') throw new IllegalArgumentException("Expected a JSON object");
        i[0]++;
        while (true) {
            i[0] = skip(json, i[0]);
            if (json.charAt(i[0]) == '}') break;
            String key = readString(json, i);
            i[0] = skip(json, i[0]);
            if (json.charAt(i[0]++) != ':') throw new IllegalArgumentException("Expected ':'");
            i[0] = skip(json, i[0]);
            String value;
            if (json.charAt(i[0]) == '"') {
                value = readString(json, i);
            } else {
                int start = i[0];
                while (i[0] < json.length() && ",} \n\r\t".indexOf(json.charAt(i[0])) < 0) i[0]++;
                value = json.substring(start, i[0]);
                if (value.equals("null")) value = null;
            }
            map.put(key, value);
            i[0] = skip(json, i[0]);
            char c = json.charAt(i[0]++);
            if (c == '}') break;
            if (c != ',') throw new IllegalArgumentException("Expected ',' or '}'");
        }
        return map;
    }

    private static int skip(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static String readString(String s, int[] i) {
        if (s.charAt(i[0]) != '"') throw new IllegalArgumentException("Expected a string");
        StringBuilder sb = new StringBuilder();
        i[0]++;
        while (s.charAt(i[0]) != '"') {
            char c = s.charAt(i[0]++);
            if (c == '\\') {
                char e = s.charAt(i[0]++);
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'u': sb.append((char) Integer.parseInt(s.substring(i[0], i[0] + 4), 16)); i[0] += 4; break;
                    default:  sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
        i[0]++;
        return sb.toString();
    }
}
