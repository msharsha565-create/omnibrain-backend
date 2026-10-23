
package com.omnibrain.rag;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmbeddingRepository extends JpaRepository<EmbeddingEntity, String> {
}
