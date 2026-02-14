package com.omnibrain.rag;

public final class VectorUtils {

    private VectorUtils() {
        // utility class
    }

    /* float[] -> String */
    public static String serialize(float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        return sb.toString();
    }

    /* String -> float[] */
    public static float[] deserialize(String data) {
        String[] parts = data.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }
}
