package io.github.maksim0840.extractionresults.repository;

import io.github.maksim0840.extractionresults.domain.ExtractionResult;
import org.springframework.data.mongodb.repository.MongoRepository;

// Репозиторий с возможностью применения пагинации и фильтрации (спецификации)
public interface ExtractionResultRepository extends
        MongoRepository<ExtractionResult, String>, ExtractionResultRepositoryCustom {
}
