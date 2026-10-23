package com.omnibrain.rag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** One chunk of an uploaded document, its vector, and who owns it. */
@Entity
@Table(name = "EMBEDDING_ENTITY",
        indexes = @Index(name = "idx_embedding_owner_embedder", columnList = "owner,embedder"))
public class EmbeddingEntity {

    @Id
    private String id;

    @Lob
    private String vector;

    @Lob
    private String text;

    private String source;

    /** Username of the uploader; retrieval is always filtered by it. Null on rows from older versions. */
    @Column(name = "owner")
    private String owner;

    /** Name of the embedding model that produced {@code vector}; vectors are only compared like with like. */
    @Column(name = "embedder")
    private String embedder;

    protected EmbeddingEntity() {
        // JPA
    }

    public EmbeddingEntity(String id, float[] vector, String text, String source,
                           String owner, String embedder) {
        this.id = id;
        this.vector = VectorUtils.serialize(vector);
        this.text = text;
        this.source = source;
        this.owner = owner;
        this.embedder = embedder;
    }

    public String getId() { return id; }
    public float[] getVector() { return VectorUtils.deserialize(vector); }
    public String getText() { return text; }
    public String getSource() { return source; }
    public String getOwner() { return owner; }
    public String getEmbedder() { return embedder; }
}
