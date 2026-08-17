package com.liwx.labproduct.service;
import com.liwx.labcommon.common.PageResult;
import com.liwx.labproduct.dto.*;
import java.util.List;

public interface ProductService {
    PageResult<ProductVO> listProducts(int page, int size, String name, Integer status);
    ProductVO getById(Long id);
    ProductVO create(ProductCreateDTO dto);
    ProductVO update(Long id, ProductUpdateDTO dto);
    void delete(Long id);
    ProductVO deductStock(Long id, int quantity);
}
