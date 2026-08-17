package com.liwx.labproduct.config;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject m) {
        this.strictInsertFill(m, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(m, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
    @Override
    public void updateFill(MetaObject m) {
        this.strictUpdateFill(m, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}