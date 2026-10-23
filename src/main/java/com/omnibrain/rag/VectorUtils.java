package com.omnibrain.rag;

import java.util.Locale;

/**
 * String form of a vector for storage. Vectors that are mostly zeros (the hashing embedder's)
 * use a compact sparse form {@code s<length>:index=value,...}; anything else, and any value written
 * before this format existed, is a plain comma-separated list.
 */
public final class VectorUtils {

    private VectorUtils() {
        // utility class
    }

    public static String serialize(float[] vector) {
        int nonZero = 0;
        for (float x : vector) if (x != 0f) nonZero++;
        StringBuilder sb = new StringBuilder();
        if (nonZero * 3 < vector.length) {
            sb.append('s').append(vector.length).append(':');
            boolean first = true;
            for (int i = 0; i < vector.length; i++) {
                if (vector[i] == 0f) continue;
                if (!first) sb.append(',');
                sb.append(i).append('=').append(vector[i]);
                first = false;
            }
            return sb.toString();
        }
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(',');
        }
        return sb.toString();
    }

    public static float[] deserialize(String data) {
        if (data.startsWith("s") && data.indexOf(':') > 0) {
            int colon = data.indexOf(':');
            float[] vector = new float[Integer.parseInt(data.substring(1, colon))];
            String body = data.substring(colon + 1);
            if (!body.isEmpty()) {
                for (String part : body.split(",")) {
                    int eq = part.indexOf('=');
                    vector[Integer.parseInt(part.substring(0, eq))] =
                            Float.parseFloat(part.substring(eq + 1).toLowerCase(Locale.ROOT));
                }
            }
            return vector;
        }
        String[] parts = data.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }
}
