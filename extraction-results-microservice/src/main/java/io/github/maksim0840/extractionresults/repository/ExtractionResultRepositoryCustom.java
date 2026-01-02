package io.github.maksim0840.extractionresults.repository;

import io.github.maksim0840.extractionresults.domain.ExtractionResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

// Описание методов, выполняющие сложные запросы
public interface ExtractionResultRepositoryCustom {
    List<ExtractionResult> searchWithFiltering(String userId, Instant dateFrom, Instant dateTo, Pageable pageable);
}
