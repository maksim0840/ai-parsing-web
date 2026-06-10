package io.github.maksim0840.extractionresults.repository;

import io.github.maksim0840.extractionresults.entity.ExtractionResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;

/**
 * Реализация методов, выполняющих сложные кастомные запросы к базе данных
 */
public class ExtractionResultRepositoryImpl implements ExtractionResultRepositoryCustom {

    // Объект для работы с низкоуровневыми запросами к MongoDB
    private final MongoTemplate mongoTemplate;

    public ExtractionResultRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // Извлекаем записи по филтрам и странице
    @Override
    public List<ExtractionResult> searchWithFilteringAndPaging(String userId, Instant dateFrom, Instant dateTo, Pageable pageable) {
        Criteria criteria = formCriteriaByFilters(userId, dateFrom, dateTo);
        Query query = new Query(criteria).with(pageable);
        return mongoTemplate.find(query, ExtractionResult.class);
    }

    // Получаем количество всех записей, найденных по филтрам
    @Override
    public long countAllWithFiltering(String userId, Instant dateFrom, Instant dateTo) {
        Criteria criteria = formCriteriaByFilters(userId, dateFrom, dateTo);
        Query query = new Query(criteria);
        return mongoTemplate.count(query, ExtractionResult.class);
    }

    private Criteria formCriteriaByFilters(String userId, Instant dateFrom, Instant dateTo) {
        Criteria criteria = new Criteria();

        if (userId != null) {
            criteria = criteria.and("userId").is(userId);
        }

        // обязательно рассмотреть вариант комбинации параметров, иначе разные условия and над одним и тем же полем будут конфликтовать друг с другом
        if (dateFrom != null && dateTo != null) {
            criteria = criteria.and("createdAt").gte(dateFrom).lte(dateTo);
        } else if (dateFrom != null) {
            criteria = criteria.and("createdAt").gte(dateFrom);  // createdDate >= dateFrom
        } else if (dateTo != null) {
            criteria = criteria.and("createdAt").lte(dateTo);  // createdDate <= dateTo
        }
        return criteria;
    }


}