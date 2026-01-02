package io.github.maksim0840.extractionresults.service;

import io.github.maksim0840.extractionresults.domain.ExtractionResult;
import io.github.maksim0840.extractionresults.exception.NotFoundException;
import io.github.maksim0840.extractionresults.repository.ExtractionResultRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ExtractionResultService {

    private final ExtractionResultRepository extractionResultsRepository;

    public ExtractionResultService(ExtractionResultRepository extractionResultsRepository) {
        this.extractionResultsRepository = extractionResultsRepository;
    }

    public ExtractionResult createExtractionResult(String url, String userId, Map<String, Object> jsonResult) {
        ExtractionResult extractionResult = new ExtractionResult(url, userId, jsonResult);
        try {
            return extractionResultsRepository.save(extractionResult);
        } catch (DataAccessException e) {
            throw new RuntimeException("MongoDB write failed", e);
        }
    }

    public ExtractionResult getExtractionResultById(String id) {
        try {
            return extractionResultsRepository.findById(id).orElseThrow(() ->
                    new NotFoundException("MongoDB's object not found (id: " + id + ")"));
        } catch (DataAccessException e) {
            throw new RuntimeException("MongoDB read failed", e);
        }
    }

    public List<ExtractionResult> getListExtractionResultBySpecPaging(String userId, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc) {
        // Настраиваем сортировку
        Sort.Direction sortDir = Sort.Direction.DESC;
        if (isSortDesc != null) sortDir = isSortDesc ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDir, "createdAt");

        // Настраиваем пагинацию
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

        // Выполняем запрос
        try {
            return extractionResultsRepository.searchWithFiltering(userId, dateFrom, dateTo, pageable);
        } catch (DataAccessException e) {
            throw new RuntimeException("MongoDB read failed", e);
        }
    }

    public void deleteExtractionResultById(String id) {
        try {
            boolean isExists = extractionResultsRepository.existsById(id);
            if (!isExists) {
                throw new NotFoundException("MongoDB's object didn't exist (id: " + id + ")");
            }

            extractionResultsRepository.deleteById(id);
        } catch (DataAccessException e) {
            throw new RuntimeException("MongoDB delete failed");
        }
    }
}
