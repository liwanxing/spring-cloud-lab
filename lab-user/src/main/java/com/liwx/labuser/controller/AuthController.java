package com.liwx.labuser.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.liwx.labcommon.common.Assert;
import com.liwx.labcommon.common.Result;
import com.liwx.labuser.dto.LoginDTO;
import com.liwx.labuser.entity.User;
import com.liwx.labuser.service.UserService;
import com.liwx.labuser.util.PasswordEncoderUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO dto) {
        User user = userService.getByUsername(dto.getUsername());
        Assert.notNull(user, "用户名或密码错误");
        Assert.isTrue(PasswordEncoderUtil.matches(dto.getPassword(), user.getPassword()),
                "用户名或密码错误");
        Assert.isTrue(user.getStatus() == 1, "账号已被禁用");

        StpUtil.login(user.getId());
        return Result.success(StpUtil.getTokenValue());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success(null);
    }

    @GetMapping("/check")
    public Result<Void> checkLogin() {
        StpUtil.checkLogin();
        return Result.success(null);
    }

    @GetMapping("/me")
    public Result<Object> getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getByIdRaw(userId);
        Assert.notNull(user, "用户不存在");
        user.setPassword(null);
        return Result.success(user);
    }
}
