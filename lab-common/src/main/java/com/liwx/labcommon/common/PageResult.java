package com.liwx.labcommon.common;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor
public class PageResult<T> {
    private List<T> records; private long total; private int page; private int size;
}