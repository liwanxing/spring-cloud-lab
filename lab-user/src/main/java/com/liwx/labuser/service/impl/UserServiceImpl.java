package com.liwx.labuser.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liwx.labuser.common.Assert;
import com.liwx.labuser.common.PageResult;
import com.liwx.labuser.dto.UserCreateDTO;
import com.liwx.labuser.dto.UserUpdateDTO;
import com.liwx.labuser.dto.UserVO;
import com.liwx.labuser.entity.User;
import com.liwx.labuser.mapper.UserMapper;
import com.liwx.labuser.service.UserService;
import com.liwx.labuser.util.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public PageResult<UserVO> listUsers(int page, int size) {
        Page<User> userPage = userMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt)
        );
        return new PageResult<>(
                userPage.getRecords().stream().map(this::toVO).toList(),
                userPage.getTotal(), page, size
        );
    }

    @Override
    public UserVO getById(Long id) {
        User user = userMapper.selectById(id);
        Assert.notNull(user, "用户不存在");
        return toVO(user);
    }

    @Override
    public UserVO create(UserCreateDTO dto) {
        Assert.isFalse(userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())) > 0,
                "用户名已存在");
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(PasswordEncoderUtil.encode(dto.getPassword()));
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);
        return toVO(user);
    }

    @Override
    public UserVO update(Long id, UserUpdateDTO dto) {
        User user = userMapper.selectById(id);
        Assert.notNull(user, "用户不存在");
        if (dto.getUsername() != null) {
            Assert.isFalse(userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, dto.getUsername())
                            .ne(User::getId, id)) > 0,
                    "用户名已存在");
        }
        BeanUtils.copyProperties(dto, user);
        if (dto.getPassword() != null) {
            user.setPassword(PasswordEncoderUtil.encode(dto.getPassword()));
        }
        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    public void delete(Long id) {
        Assert.isTrue(userMapper.selectById(id) != null, "用户不存在");
        userMapper.deleteById(id);
    }

    @Override
    public List<UserVO> search(String username, Integer status) {
        return userMapper.searchByUsername(username, status)
                .stream().map(this::toVO).toList();
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return PasswordEncoderUtil.matches(rawPassword, encodedPassword);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
