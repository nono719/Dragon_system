package com.cuit.academic.common;

import com.github.pagehelper.PageInfo;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private int pageNum;
    private int pageSize;
    private List<T> list;

    public static <T> PageResult<T> of(PageInfo<T> page) {
        PageResult<T> r = new PageResult<>();
        r.total = page.getTotal();
        r.pageNum = page.getPageNum();
        r.pageSize = page.getPageSize();
        r.list = page.getList();
        return r;
    }
}
