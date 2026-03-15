package com.english.learn.repository;

import com.english.learn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户表数据访问层。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 按用户名查询，用于登录与唯一性校验。
     */
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
