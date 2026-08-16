package com.liwx.labuser.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.liwx.labuser.common.Assert;
import com.liwx.labuser.common.Result;
import com.liwx.labuser.dto.LoginDTO;
import com.liwx.labuser.entity.User;
import com.liwx.labuser.mapper.UserMapper;
import com.liwx.labuser.util.PasswordEncoderUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        Assert.notNull(user, "用户名或密码错误");
        Assert.isTrue(PasswordEncoderUtil.matches(dto.getPassword(), user.getPassword()),
                "用户名或密码错误");

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
}
