// LOCKED VERSION — DO NOT MODIFY
// Verified end-to-end: upload → embed → persist → retrieve → answer

package com.omnibrain.rag;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmbeddingRepository extends JpaRepository<EmbeddingEntity, String> {
}
