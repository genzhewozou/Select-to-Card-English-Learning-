package com.english.learn.controller;

import com.english.learn.common.Result;
import com.english.learn.dto.LoginRequest;
import com.english.learn.dto.UserDTO;
import com.english.learn.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户 REST 接口：注册、登录、查询、更新、删除。
 * 路径前缀：/api（由 context-path 统一配置）
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** POST /api/user/register */
    @PostMapping("/register")
    public Result<UserDTO> register(@Valid @RequestBody UserDTO dto) {
        return Result.success(userService.register(dto));
    }

    /** POST /api/user/login */
    @PostMapping("/login")
    public Result<UserDTO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    /** GET /api/user/{id} */
    @GetMapping("/{id}")
    public Result<UserDTO> getById(@PathVariable Long id) {
        return userService.getById(id)
                .map(Result::success)
                .orElse(Result.fail(404, "用户不存在"));
    }

    /** GET /api/user/list */
    @GetMapping("/list")
    public Result<List<UserDTO>> list() {
        return Result.success(userService.listAll());
    }

    /** PUT /api/user/{id} */
    @PutMapping("/{id}")
    public Result<UserDTO> update(@PathVariable Long id, @RequestBody UserDTO dto) {
        return Result.success(userService.update(id, dto));
    }

    /** DELETE /api/user/{id} */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteById(id);
        return Result.success();
    }
}
