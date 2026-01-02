package io.github.maksim0840.extractionresults.repository;

import io.github.maksim0840.extractionresults.domain.ExtractionResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;

// Реализация методов, выполняющие сложные кастомные запросы
public class ExtractionResultRepositoryImpl implements ExtractionResultRepositoryCustom {

    // Объект для работы с низкоуровневыми запросами к MongoDB
    private final MongoTemplate mongoTemplate;

    public ExtractionResultRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<ExtractionResult> searchWithFiltering(String userId, Instant dateFrom, Instant dateTo, Pageable pageable) {
        Criteria criteria = new Criteria();
        if (userId != null) criteria = criteria.and("userId").is(userId);
        if (dateFrom != null) criteria = criteria.and("createdDate").gte(dateFrom);  // createdDate >= dateFrom
        if (dateTo != null) criteria = criteria.and("createdDate").lte(dateTo);  // createdDate <= dateTo

        Query query = new Query(criteria).with(pageable);

        List<ExtractionResult> extractionResults = mongoTemplate.find(query, ExtractionResult.class);
        return extractionResults;
    }
}
