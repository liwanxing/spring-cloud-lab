package com.liwx.labuser.service;

import com.liwx.labcommon.common.PageResult;
import com.liwx.labuser.dto.UserCreateDTO;
import com.liwx.labuser.dto.UserUpdateDTO;
import com.liwx.labuser.dto.UserVO;
import com.liwx.labuser.entity.User;

public interface UserService {
    PageResult<UserVO> listUsers(int page, int size, String username, Integer status);
    UserVO getById(Long id);
    UserVO create(UserCreateDTO dto);
    UserVO update(Long id, UserUpdateDTO dto);
    void delete(Long id);

    User getByUsername(String username);

    User getByIdRaw(Long id);
}
