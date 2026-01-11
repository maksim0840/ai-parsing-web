package io.github.maksim0840.usersinfo.service;

import io.github.maksim0840.usersinfo.domain.User;
import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import io.github.maksim0840.usersinfo.exception.EncryptionException;
import io.github.maksim0840.usersinfo.exception.EncryptionIllegalArgumentException;
import io.github.maksim0840.usersinfo.exception.NotFoundException;
import io.github.maksim0840.usersinfo.repository.UserRepository;
import io.github.maksim0840.usersinfo.repository.UserSpecification;
import io.github.maksim0840.usersinfo.utils.PasswordEncryption;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String name, String password, UserRole role) {
        String passwordHash;
        try {
            passwordHash = PasswordEncryption.makeHash(password);
        } catch (IllegalArgumentException e) {
            throw new EncryptionIllegalArgumentException("User's password illegal argument", e);
        } catch (RuntimeException e) {
            throw new EncryptionException("User's password encrypt failed", e);
        }

        User user = new User(name, passwordHash, role);
        try {
            return userRepository.save(user);
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL user write failed", e);
        }
    }

    public User getUserById(Long id) {
        try {
            return userRepository.findById(id).orElseThrow(() ->
                    new NotFoundException("PostgreSQL user not found (id: " + id + ")"));
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL user read failed", e);
        }
    }

    public List<User> getListUserByPageWithFiltering(UserRole role, Instant dateFrom, Instant dateTo, int pageNum, int pageSize, Boolean isSortDesc) {
        // Настраиваем фильтрацию
        Specification<User> spec = Specification.where(null);
        if (dateFrom != null) spec = spec.and(UserSpecification.greaterOrEqualCreatedAt(dateFrom));
        if (dateTo != null) spec = spec.and(UserSpecification.lessOrEqualCreatedAt(dateTo));
        if (role != null) spec = spec.and(UserSpecification.hasRole(role));

        // Настраиваем сортировку
        Sort.Direction sortDir = Sort.Direction.DESC;
        if (isSortDesc != null) sortDir = isSortDesc ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDir, "createdAt");

        // Настраиваем пагинацию
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

        // Выполняем запрос
        try {
            return userRepository.findAll(spec, pageable).getContent();
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL user read failed", e);
        }
    }

    public void deleteUserById(Long id) {
        try {
            userRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("PostgreSQL user didn't exist (id: " + id + ")");
        } catch (DataAccessException e) {
            throw new RuntimeException("PostgreSQL user delete failed", e);
        }
    }

    @Transactional
    public User setUserRoleById(Long id, UserRole role) {
        User user = getUserById(id);
        user.setRole(role);
        return user;
    }

    public boolean checkUserPasswordById(Long id, String password) {
        User user = getUserById(id);
        String passwordHash = user.getPasswordHash();
        try {
            return PasswordEncryption.checkMatching(password, passwordHash);
        } catch (IllegalArgumentException e) {
            throw new EncryptionIllegalArgumentException("User's password illegal argument", e);
        } catch (RuntimeException e) {
            throw new EncryptionException("User's password check failed", e);
        }
    }
}
