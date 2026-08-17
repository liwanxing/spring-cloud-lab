package com.liwx.labproduct;

import com.liwx.labproduct.dto.ProductCreateDTO;
import com.liwx.labproduct.dto.ProductVO;
import com.liwx.labproduct.service.ProductService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LabProductApplicationTests {

    @Autowired
    private ProductService productService;

    @Test @Order(1) @DisplayName("创建商品")
    void testCreate() {
        ProductCreateDTO dto = ProductCreateDTO.builder()
                .name("测试商品").price(new BigDecimal("99.99")).stock(10).category("测试").build();
        ProductVO vo = productService.create(dto);
        assertNotNull(vo.getId());
        assertEquals("测试商品", vo.getName());
    }

    @Test @Order(2) @DisplayName("查询商品")
    void testGetById() {
        ProductCreateDTO dto = ProductCreateDTO.builder()
                .name("查询商品").price(new BigDecimal("19.99")).stock(5).build();
        ProductVO created = productService.create(dto);
        ProductVO found = productService.getById(created.getId());
        assertEquals("查询商品", found.getName());
    }

    @Test @Order(3) @DisplayName("分页查询")
    void testList() {
        for (int i = 0; i < 15; i++) {
            productService.create(ProductCreateDTO.builder()
                    .name("商品" + i).price(new BigDecimal("10.00")).stock(10).build());
        }
        var result = productService.listProducts(1, 10, null, null);
        assertEquals(10, result.getRecords().size());
        assertTrue(result.getTotal() >= 15);
    }

    @Test @Order(4) @DisplayName("扣减库存")
    void testDeductStock() {
        ProductCreateDTO dto = ProductCreateDTO.builder()
                .name("库存商品").price(new BigDecimal("10.00")).stock(10).build();
        ProductVO created = productService.create(dto);
        ProductVO after = productService.deductStock(created.getId(), 3);
        assertEquals(7, after.getStock());
    }

    @Test @Order(5) @DisplayName("库存不足")
    void testDeductStockInsufficient() {
        ProductCreateDTO dto = ProductCreateDTO.builder()
                .name("库存不足商品").price(new BigDecimal("10.00")).stock(2).build();
        ProductVO created = productService.create(dto);
        assertThrows(Exception.class, () -> productService.deductStock(created.getId(), 5));
    }
}
