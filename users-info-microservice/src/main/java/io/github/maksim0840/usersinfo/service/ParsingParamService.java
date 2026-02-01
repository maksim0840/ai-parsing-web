package io.github.maksim0840.usersinfo.service;

import io.github.maksim0840.usersinfo.domain.ParsingParam;
import io.github.maksim0840.usersinfo.domain.User;
import io.github.maksim0840.usersinfo.exception.NotFoundException;
import io.github.maksim0840.usersinfo.repository.ParsingParamRepository;
import io.github.maksim0840.usersinfo.repository.ParsingParamSpecification;
import io.github.maksim0840.usersinfo.repository.UserRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ParsingParamService {

    private final UserRepository userRepository;
    private final ParsingParamRepository parsingParamRepository;

    public ParsingParamService(UserRepository userRepository, ParsingParamRepository parsingParamRepository) {
        this.userRepository = userRepository;
        this.parsingParamRepository = parsingParamRepository;
    }

    public ParsingParam createParsingParam(Long userId, String name, String description) {
        ParsingParam parsingParam = new ParsingParam(getUserById(userId), name, description);
        try {
            return parsingParamRepository.save(parsingParam);
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL parsingParam write failed", e);
        }
    }

    public ParsingParam getParsingParamById(Long id) {
        try {
            return parsingParamRepository.findById(id).orElseThrow(() ->
                    new NotFoundException("PostgreSQL parsingParam not found (id: " + id + ")"));
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL parsingParam read failed", e);
        }
    }

    public List<ParsingParam> getListParsingParamByPageWithFiltering(Long userId, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc) {
        // Настраиваем фильтрацию
        Specification<ParsingParam> spec = Specification.where(null);
        if (dateFrom != null) spec = spec.and(ParsingParamSpecification.greaterOrEqualCreatedAt(dateFrom));
        if (dateTo != null) spec = spec.and(ParsingParamSpecification.lessOrEqualCreatedAt(dateTo));
        if (userId != null) spec = spec.and(ParsingParamSpecification.hasUser(userId));

        // Настраиваем сортировку
        Sort.Direction sortDir = Sort.Direction.DESC;
        if (isSortDesc != null) sortDir = isSortDesc ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDir, "createdAt");

        // Настраиваем пагинацию
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

        // Выполняем запрос
        try {
            return parsingParamRepository.findAll(spec, pageable).getContent();
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL parsingParam read failed", e);
        }
    }

    public void deleteParsingParamById(Long id) {
        if (!checkExistenceParsingParamById(id)) {
            throw new NotFoundException("PostgreSQL parsingParam didn't exist (id: " + id + ")");
        }
        try {
            parsingParamRepository.deleteById(id);
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL parsingParam delete failed", e);
        }
    }

    private boolean checkExistenceParsingParamById(Long id) {
        try {
            return parsingParamRepository.existsById(id);
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL parsingParam check existence failed", e);
        }
    }

    private User getUserById(Long userId) {
        try {
            return userRepository.findById(userId).orElseThrow(() ->
                    new NotFoundException("PostgreSQL user not found (id: " + userId + ")"));
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL user read failed", e);
        }
    }
}
