package io.github.maksim0840.usersinfo.service;

import io.github.maksim0840.internalapi.user.v1.dto.HtmlParserParamsDTO;
import io.github.maksim0840.internalapi.user.v1.dto.HtmlPreprocessingParamsDTO;
import io.github.maksim0840.internalapi.user.v1.dto.LLMParamsDTO;
import io.github.maksim0840.internalapi.user.v1.dto.ParsingParamDTO;
import io.github.maksim0840.usersinfo.entity.ParsingParam;
import io.github.maksim0840.usersinfo.entity.User;
import io.github.maksim0840.usersinfo.entity.model.HtmlParserParams;
import io.github.maksim0840.usersinfo.entity.model.HtmlPreprocessingParams;
import io.github.maksim0840.usersinfo.entity.model.LLMParams;
import io.github.maksim0840.usersinfo.exception.NotFoundException;
import io.github.maksim0840.usersinfo.mapper.ParsingParamMapper;
import io.github.maksim0840.usersinfo.mapper.UserMapper;
import io.github.maksim0840.usersinfo.repository.ParsingParamRepository;
import io.github.maksim0840.usersinfo.repository.ParsingParamSpecification;
import io.github.maksim0840.usersinfo.repository.UserRepository;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.dao.DataAccessException;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ParsingParamService {

    private final UserRepository userRepository;
    private final ParsingParamRepository parsingParamRepository;
    private final ParsingParamMapper parsingParamMapper;

    public ParsingParamService(UserRepository userRepository, ParsingParamRepository parsingParamRepository, ParsingParamMapper parsingParamMapper) {
        this.userRepository = userRepository;
        this.parsingParamRepository = parsingParamRepository;
        this.parsingParamMapper = parsingParamMapper;
    }

    public ParsingParamDTO createParsingParam(Long userId, String name, HtmlParserParamsDTO htmlParserParams, HtmlPreprocessingParamsDTO htmlPreprocessingParams, LLMParamsDTO llmParams) {
        if (parsingParamRepository.findByUserIdAndName(userId, name).isPresent()) {
            throw new IllegalArgumentException("PostgreSQL record already exists (userId:" + userId + ", name: " + name + ")");
        }
        ParsingParam parsingParam = new ParsingParam(
                getUserRaw(userId),
                name,
                parsingParamMapper.toEntity(htmlParserParams),
                parsingParamMapper.toEntity(htmlPreprocessingParams),
                parsingParamMapper.toEntity(llmParams)
        );
        try {
            parsingParam = parsingParamRepository.save(parsingParam);
            return parsingParamMapper.toDto(parsingParam);
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL parsingParam write failed", e);
        }
    }

    public ParsingParamDTO editParsingParam(Long id, Long userId, String name, HtmlParserParamsDTO htmlParserParams, HtmlPreprocessingParamsDTO htmlPreprocessingParams, LLMParamsDTO llmParams) {
        ParsingParam parsingParam = parsingParamRepository.findById(id).orElseThrow(() ->
                new NotFoundException("PostgreSQL parsingParam not found (id: " + id + ")"));
        parsingParam.setName(name);
        parsingParam.setHtmlParserParams(parsingParamMapper.toEntity(htmlParserParams));
        parsingParam.setHtmlPreprocessingParams(parsingParamMapper.toEntity(htmlPreprocessingParams));
        parsingParam.setLlmParams(parsingParamMapper.toEntity(llmParams));

        try {
            parsingParam = parsingParamRepository.save(parsingParam);
            return parsingParamMapper.toDto(parsingParam);
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL parsingParam write failed", e);
        }

    }

    public ParsingParamDTO getParsingParamById(Long id) {
        try {
            ParsingParam parsingParam = parsingParamRepository.findById(id).orElseThrow(() ->
                    new NotFoundException("PostgreSQL parsingParam not found (id: " + id + ")"));
            return parsingParamMapper.toDto(parsingParam);
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL parsingParam read failed", e);
        }
    }

    public List<ParsingParamDTO> getListParsingParamByPageWithFiltering(Long userId, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc) {
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
            List<ParsingParam> parsingParams = parsingParamRepository.findAll(spec, pageable).getContent();
            return parsingParams.stream().map(parsingParamMapper::toDto).toList();
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

    public List<String> getListParsingParamNameByUserId(Long userId) {
        try {
            return parsingParamRepository.findNamesByUserId(userId);
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL parsingParam read failed", e);
        }
    }

    public ParsingParamDTO getParsingParamByUserIdAndName(Long userId, String name) {
        try {
            ParsingParam parsingParam = parsingParamRepository.findByUserIdAndName(userId, name).orElseThrow(() ->
                    new NotFoundException("PostgreSQL parsingParam not found (userId: " + userId + ", name: " + name + ")"));
            return parsingParamMapper.toDto(parsingParam);
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL parsingParam read failed", e);
        }
    }

    public void renameParsingParam(Long userId, String oldName, String newName) {
        if (parsingParamRepository.findByUserIdAndName(userId, newName).isPresent()) {
            throw new IllegalArgumentException("PostgreSQL record already exists (userId:" + userId + ", name: " + newName + ")");
        }
        try {
            parsingParamRepository.renameByUserIdAndName(userId, oldName, newName);
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL parsingParam rename failed", e);
        }
    }

    public void deleteParsingParamByUserIdAndName(Long userId, String name) {
        if (parsingParamRepository.findByUserIdAndName(userId, name).isEmpty()) {
            throw new NotFoundException("PostgreSQL parsingParam didn't exist (userId:" + userId + ", name: " + name + ")");
        }
        try {
            parsingParamRepository.deleteByUserIdAndName(userId, name);
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

    private User getUserRaw(Long userId) {
        try {
            return userRepository.findById(userId).orElseThrow(() ->
                    new NotFoundException("PostgreSQL user not found (id: " + userId + ")"));
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL user read failed", e);
        }
    }
}
