package com.english.learn;

import com.english.learn.config.DocumentStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 英语学习系统启动类。
 * 遵循阿里巴巴规范：启动类置于 groupId 根包下。
 *
 * @author system
 */
@SpringBootApplication
@EnableConfigurationProperties(DocumentStorageProperties.class)
public class EnglishLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishLearningApplication.class, args);
    }
}
