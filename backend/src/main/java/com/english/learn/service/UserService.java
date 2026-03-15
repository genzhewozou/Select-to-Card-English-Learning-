package com.english.learn.service;

import com.english.learn.dto.LoginRequest;
import com.english.learn.dto.UserDTO;
import com.english.learn.entity.User;
import com.english.learn.mapper.UserMapper;
import com.english.learn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户业务服务：注册、登录、CRUD。
 * 密码暂明文存储，生产环境需改为 BCrypt 等加密。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(rollbackFor = Exception.class)
    public UserDTO register(UserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User entity = UserMapper.toEntity(dto);
        entity = userRepository.save(entity);
        return UserMapper.toDTO(entity);
    }

    public UserDTO login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return UserMapper.toDTO(user);
    }

    public Optional<UserDTO> getById(Long id) {
        return userRepository.findById(id).map(UserMapper::toDTO);
    }

    public List<UserDTO> listAll() {
        return userRepository.findAll().stream().map(UserMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public UserDTO update(Long id, UserDTO dto) {
        User entity = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (dto.getNickname() != null) {
            entity.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            entity.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            entity.setPassword(dto.getPassword());
        }
        entity = userRepository.save(entity);
        return UserMapper.toDTO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
