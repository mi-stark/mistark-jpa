package com.mistark.data.jpa.support;

import java.util.List;

public interface BaseMapper<T> {
    int inert(T entity);
    int deleteById(T entity);
    int updateById(T entity);
    List<T> selectList();
    T selectById(T entity);
}
