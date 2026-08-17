package com.liwx.labuser.config;

import cn.dev33.satoken.stp.StpInterface;
import com.liwx.labuser.entity.User;
import com.liwx.labuser.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {
    private final UserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = userMapper.selectById(Long.parseLong(loginId.toString()));
        return user != null ? List.of(user.getRole()) : List.of();
    }
}
