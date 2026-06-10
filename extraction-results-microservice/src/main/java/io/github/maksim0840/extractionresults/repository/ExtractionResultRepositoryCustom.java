package io.github.maksim0840.extractionresults.repository;

import io.github.maksim0840.extractionresults.entity.ExtractionResult;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

/**
 * Описание методов для обработки сложных запросов к базе данных
 */
public interface ExtractionResultRepositoryCustom {
    List<ExtractionResult> searchWithFilteringAndPaging(String userId, Instant dateFrom, Instant dateTo, Pageable pageable);
    long countAllWithFiltering(String userId, Instant dateFrom, Instant dateTo);
}
