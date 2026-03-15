package com.english.learn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户表实体。
 * 存储系统用户基本信息，用于登录与文档、卡片归属。
 */
@Data
@Entity
@Table(name = "learn_user", indexes = {
    @Index(name = "uk_username", columnList = "username", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录用户名，唯一 */
    @Column(nullable = false, length = 64)
    private String username;

    /** 密码（存储时需加密） */
    @Column(nullable = false, length = 128)
    private String password;

    /** 昵称/显示名 */
    @Column(length = 64)
    private String nickname;

    /** 邮箱 */
    @Column(length = 128)
    private String email;

    @Column(name = "gmt_create", nullable = false, updatable = false)
    private LocalDateTime gmtCreate;

    @Column(name = "gmt_modified", nullable = false)
    private LocalDateTime gmtModified;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.gmtCreate = now;
        this.gmtModified = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.gmtModified = LocalDateTime.now();
    }
}
