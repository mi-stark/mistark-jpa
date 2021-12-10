package com.mistark.data.jpa.binding;

import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Method;

public interface JpaMapperMethod {
    Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable;
}
