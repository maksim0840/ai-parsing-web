package io.github.maksim0840.usersinfo.mapper;

import io.github.maksim0840.internalapi.user.v1.dto.UserDTO;
import io.github.maksim0840.usersinfo.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDto(User user);

    @Mapping(target = "parsingParams", ignore = true)
    User toEntity(UserDTO dto); // пропустит параметр parsingParams
}
