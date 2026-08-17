package com.liwx.labuser.controller;

import com.liwx.labuser.common.PageResult;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.liwx.labuser.common.Result;
import com.liwx.labuser.dto.UserCreateDTO;
import com.liwx.labuser.dto.UserUpdateDTO;
import com.liwx.labuser.dto.UserVO;
import com.liwx.labuser.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SaCheckRole("admin")
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<PageResult<UserVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.listUsers(page, size, username, status));
    }

    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping
    public Result<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success(userService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return Result.success(userService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success(null);
    }

}
