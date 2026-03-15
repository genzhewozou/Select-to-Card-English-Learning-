package com.english.learn.mapper;

import com.english.learn.dto.UserDTO;
import com.english.learn.entity.User;
import org.springframework.beans.BeanUtils;

/**
 * 用户 Entity 与 DTO 转换。不暴露密码到 DTO。
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static User toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        User entity = new User();
        BeanUtils.copyProperties(dto, entity, "password");
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            entity.setPassword(dto.getPassword()); // 实际应使用加密
        }
        return entity;
    }

    public static UserDTO toDTO(User entity) {
        if (entity == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(entity, dto, "password");
        return dto;
    }
}
