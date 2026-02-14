package com.omnibrain.rag;

import java.util.List;

public class AskResponse {

    public String answer;
    public double confidence;
    public String confidenceLabel;
    public List<SourceHit> sources;

    public AskResponse(
            String answer,
            double confidence,
            String confidenceLabel,
            List<SourceHit> sources
    ) {
        this.answer = answer;
        this.confidence = confidence;
        this.confidenceLabel = confidenceLabel;
        this.sources = sources;
    }

    public static class SourceHit {
        public double score;
        public String text;

        public SourceHit(double score, String text) {
            this.score = score;
            this.text = text;
        }
    }
}
