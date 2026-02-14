// LOCKED VERSION — DO NOT MODIFY
// Verified end-to-end: upload → embed → persist → retrieve → answer

package com.omnibrain.rag;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "EMBEDDING_ENTITY")
public class EmbeddingEntity {

    @Id
    private String id;

    @Lob
    private String vector;

    @Lob
    private String text;

    // ✅ THIS FIELD WAS MISSING
    private String source;

    protected EmbeddingEntity() {
        // JPA
    }

    public EmbeddingEntity(String id, float[] vector, String text, String source) {
        this.id = id;
        this.vector = VectorUtils.serialize(vector);
        this.text = text;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public float[] getVector() {
        return VectorUtils.deserialize(vector);
    }

    public String getText() {
        return text;
    }

    // ✅ NOW THIS WORKS
    public String getSource() {
        return source;
    }
}
