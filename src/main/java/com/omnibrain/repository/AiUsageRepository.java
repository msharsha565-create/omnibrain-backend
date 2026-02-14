package com.omnibrain.repository;

import com.omnibrain.entity.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AiUsageRepository extends JpaRepository<AiUsage, Long> {

    long countByUserIdAndCreatedAtAfter(String userId, LocalDateTime time);
}
