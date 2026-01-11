package io.github.maksim0840.extractionresults.repository;

import io.github.maksim0840.extractionresults.domain.ExtractionResult;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Основной репозиторий с возможностью отправки запросов к MongoDB (MongoRepository)
 * и выполнения кастомных методов сложной фильтрации (ExtractionResultRepositoryCustom)
 */
public interface ExtractionResultRepository extends
        MongoRepository<ExtractionResult, String>, ExtractionResultRepositoryCustom {
}
