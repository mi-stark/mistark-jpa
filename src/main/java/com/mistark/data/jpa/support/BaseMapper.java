package com.mistark.data.jpa.support;

import com.mistark.data.jpa.meta.Query;

import java.util.List;

public interface BaseMapper<T> {
    int insert(T entity);
    int deleteById(T entity);
    int updateById(T entity);
    List<T> queryList(Object...args);
    T queryById(T entity);
}
