package com.omnibrain.rag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmbeddingRepository extends JpaRepository<EmbeddingEntity, String> {

    interface DocumentSummary {
        String getSource();
        long getChunks();
    }

    List<EmbeddingEntity> findByOwnerAndEmbedder(String owner, String embedder);

    long countByOwner(String owner);

    long deleteByOwnerAndSource(String owner, String source);

    @Query("select e.source as source, count(e) as chunks from EmbeddingEntity e "
            + "where e.owner = :owner group by e.source order by e.source")
    List<DocumentSummary> summarize(@Param("owner") String owner);
}
