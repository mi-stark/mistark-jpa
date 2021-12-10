package com.mistark.data.jpa.binding;

import org.apache.ibatis.binding.MapperProxyFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.util.Map;

public class JpaMapperProxyFactory<T> extends MapperProxyFactory {

    private final Class<T> jpaMapper;
    private final Map jpaMethodCache;
    private List<JpaMapperMethodFactory> methodFactories;

    public JpaMapperProxyFactory(Class<T> mapperInterface) {
        super(mapperInterface);
        this.jpaMapper = mapperInterface;
        MetaObject metaObject = SystemMetaObject.forObject(this);
        this.jpaMethodCache = (Map)metaObject.getValue("methodCache");
    }

    @Override
    public Object newInstance(SqlSession sqlSession) {
        final JpaMapperProxy jpaMapperProxy = new JpaMapperProxy(sqlSession, jpaMapper, jpaMethodCache, methodFactories);
        return super.newInstance(jpaMapperProxy);
    }

    public void setMethodFactories(List<JpaMapperMethodFactory> methodFactories) {
        this.methodFactories = methodFactories;
    }
}
