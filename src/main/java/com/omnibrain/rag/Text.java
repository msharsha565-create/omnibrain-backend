package com.omnibrain.rag;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Shared text utilities: normalisation, tokenisation (with light stemming) and sentence splitting. */
public final class Text {

    private Text() {}

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "been", "but", "by", "can", "do", "does",
            "for", "from", "has", "have", "how", "i", "if", "in", "is", "it", "its", "of", "on",
            "or", "so", "than", "that", "the", "their", "then", "there", "these", "they", "this",
            "to", "was", "were", "what", "when", "where", "which", "who", "why", "will", "with",
            "you", "your", "about", "into", "not", "no", "we", "our", "should", "would", "could");

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+(?=[\\p{Lu}\\p{N}\"'(\\[])");

    /** NFC-normalise, drop control characters other than newline and tab, collapse runs of spaces. */
    public static String clean(String raw) {
        String s = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFC);
        s = s.replace("\r\n", "\n").replace('\r', '\n');
        s = s.replaceAll("[\\p{Cc}&&[^\\n\\t]]", " ");
        s = s.replaceAll("[ \\t]+", " ");
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s.trim();
    }

    /** Lower-cased, stopword-free, lightly stemmed tokens. */
    public static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        var m = WORD.matcher(text == null ? "" : text.toLowerCase(Locale.ROOT));
        while (m.find()) {
            String w = m.group();
            if (w.length() < 2 || STOPWORDS.contains(w)) continue;
            out.add(stem(w));
        }
        return out;
    }

    /**
     * A deliberately small suffix stripper (not Porter): enough that "brewing", "brewed" and
     * "brews" meet, applied identically to questions and documents.
     */
    static String stem(String w) {
        if (w.length() > 6 && w.endsWith("ions")) return w.substring(0, w.length() - 4);
        if (w.length() > 5 && w.endsWith("ion")) return w.substring(0, w.length() - 3);
        if (w.length() > 5 && w.endsWith("oes")) return w.substring(0, w.length() - 2);
        if (w.length() > 5 && w.endsWith("ing")) return w.substring(0, w.length() - 3);
        if (w.length() > 4 && w.endsWith("ies")) return w.substring(0, w.length() - 3) + "y";
        if (w.length() > 4 && w.endsWith("ed")) return w.substring(0, w.length() - 2);
        if (w.length() > 4 && (w.endsWith("sses") || w.endsWith("shes") || w.endsWith("ches")
                || w.endsWith("xes"))) return w.substring(0, w.length() - 2);
        if (w.length() > 3 && w.endsWith("s") && !w.endsWith("ss") && !w.endsWith("us")) {
            return w.substring(0, w.length() - 1);
        }
        return w;
    }

    public static List<String> sentences(String text) {
        List<String> out = new ArrayList<>();
        for (String s : SENTENCE_END.split(text.replace('\n', ' ').trim())) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
