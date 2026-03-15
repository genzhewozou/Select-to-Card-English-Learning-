package com.english.learn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 英语学习系统启动类。
 * 遵循阿里巴巴规范：启动类置于 groupId 根包下。
 *
 * @author system
 */
@SpringBootApplication
public class EnglishLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishLearningApplication.class, args);
    }
}
