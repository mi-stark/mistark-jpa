package com.mistark.data.jpa.binding;

import org.apache.ibatis.binding.MapperProxy;
import org.apache.ibatis.session.SqlSession;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JpaMapperProxy extends MapperProxy {

    private final Map<Method, JpaMapperMethod> jpaMethodCache = new ConcurrentHashMap<>();
    private final SqlSession jpaSqlSession;
    private final Class mapperInterface;
    private final List<JpaMapperMethodFactory> methodFactories;

    public JpaMapperProxy(SqlSession sqlSession, Class mapperInterface, Map methodCache, List<JpaMapperMethodFactory> methodFactories) {
        super(sqlSession, mapperInterface, methodCache);
        this.mapperInterface = mapperInterface;
        this.jpaSqlSession = sqlSession;
        this.methodFactories = methodFactories;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        JpaMapperMethod jpaMapperMethod = getCachedJpaMethod(method);
        if(jpaMapperMethod!=null){
            return jpaMapperMethod.invoke(proxy, method, args, jpaSqlSession);
        }
        return super.invoke(proxy, method, args);
    }

    private JpaMapperMethod getCachedJpaMethod(Method method){
        if(CollectionUtils.isEmpty(methodFactories)) return null;
        JpaMapperMethod jpaMapperMethod = null;
        if(!jpaMethodCache.containsKey(method)){
            for (JpaMapperMethodFactory factory: methodFactories){
                if(!factory.match(jpaSqlSession, mapperInterface, method)) continue;
                jpaMapperMethod = factory.newInstance(jpaSqlSession, mapperInterface, method);
                jpaMethodCache.put(method, jpaMapperMethod);
                break;
            }
        }else {
            jpaMapperMethod = jpaMethodCache.get(method);
        }
        return jpaMapperMethod;
    }

}
