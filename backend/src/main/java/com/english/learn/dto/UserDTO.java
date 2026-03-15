package com.english.learn.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 用户请求/响应 DTO。
 */
@Data
public class UserDTO {

    private Long id;
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 64)
    private String username;
    @Size(min = 6, max = 32, message = "密码长度 6-32")
    private String password;
    @Size(max = 64)
    private String nickname;
    @Size(max = 128)
    private String email;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
