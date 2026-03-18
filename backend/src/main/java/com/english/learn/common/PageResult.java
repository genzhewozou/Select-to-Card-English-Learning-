package com.english.learn.common;

import lombok.Data;

import java.util.List;

/**
 * 通用分页返回结构。
 */
@Data
public class PageResult<T> {

    /** 当前页（从 1 开始） */
    private int page;
    /** 每页大小 */
    private int size;
    /** 总条数 */
    private long total;
    /** 当前页数据 */
    private List<T> list;

    public static <T> PageResult<T> of(int page, int size, long total, List<T> list) {
        PageResult<T> r = new PageResult<>();
        r.setPage(page);
        r.setSize(size);
        r.setTotal(total);
        r.setList(list);
        return r;
    }
}

