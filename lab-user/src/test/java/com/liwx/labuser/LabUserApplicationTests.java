package com.liwx.labuser;

import com.liwx.labuser.dto.UserCreateDTO;
import com.liwx.labuser.dto.UserVO;
import com.liwx.labuser.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional  // 每个测试方法结束后自动回滚，不污染数据库；去掉此注解则正常提交
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LabUserApplicationTests {

    @Autowired
    private UserService userService;


    @Test
    @Order(1)
    @DisplayName("创建用户-密码加密")
    void testCreateUser() {
        UserCreateDTO dto = UserCreateDTO.builder()
                .username("testuser")
                .password("123456")
                .email("test@example.com")
                .phone("13800138000")
                .build();
        UserVO vo = userService.create(dto);
        assertNotNull(vo.getId());
        assertEquals("testuser", vo.getUsername());
    }

    @Test
    @Order(3)
    @DisplayName("查询用户")
    void testGetUser() {
        UserCreateDTO dto = UserCreateDTO.builder()
                .username("queryuser")
                .password("123456")
                .build();
        UserVO created = userService.create(dto);
        UserVO found = userService.getById(created.getId());
        assertEquals("queryuser", found.getUsername());
    }

    @Test
    @Order(4)
    @DisplayName("用户名重复校验")
    void testDuplicateUsername() {
        UserCreateDTO dto = UserCreateDTO.builder()
                .username("dupuser")
                .password("123456")
                .build();
        userService.create(dto);
        assertThrows(Exception.class, () -> userService.create(dto));
    }

    @Test
    @Order(5)
    @DisplayName("分页查询")
    void testListUsers() {
        for (int i = 0; i < 15; i++) {
            UserCreateDTO dto = UserCreateDTO.builder()
                    .username("pageuser" + i)
                    .password("123456")
                    .build();
            userService.create(dto);
        }
        var result = userService.listUsers(1, 10, null, null);
        assertEquals(10, result.getRecords().size());
        assertTrue(result.getTotal() >= 15);
    }

    @Test
    @Order(6)
    @DisplayName("删除用户")
    void testDeleteUser() {
        UserCreateDTO dto = UserCreateDTO.builder()
                .username("deluser")
                .password("123456")
                .build();
        UserVO created = userService.create(dto);
        userService.delete(created.getId());
        assertThrows(Exception.class, () -> userService.getById(created.getId()));
    }

}
