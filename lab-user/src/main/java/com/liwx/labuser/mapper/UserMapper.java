package com.liwx.labuser.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liwx.labuser.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    List<User> searchByUsername(@Param("username") String username, @Param("status") Integer status);
}
