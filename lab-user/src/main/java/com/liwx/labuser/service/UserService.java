package com.liwx.labuser.service;

import com.liwx.labuser.common.PageResult;
import com.liwx.labuser.dto.UserCreateDTO;
import com.liwx.labuser.dto.UserUpdateDTO;
import com.liwx.labuser.dto.UserVO;
import java.util.List;

public interface UserService {
    PageResult<UserVO> listUsers(int page, int size);
    UserVO getById(Long id);
    UserVO create(UserCreateDTO dto);
    UserVO update(Long id, UserUpdateDTO dto);
    void delete(Long id);
    List<UserVO> search(String username, Integer status);
}
