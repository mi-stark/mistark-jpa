package com.mistark.data.jpa.binding;

import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Method;

public interface JpaMapperMethodFactory {
    boolean match(SqlSession sqlSession, Class mapperInterface, Method method);
    JpaMapperMethod newInstance(SqlSession sqlSession, Class mapperInterface, Method method);
}
