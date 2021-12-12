package com.mistark.data.jpa.binding;

import com.mistark.meta.Value;
import org.apache.ibatis.binding.MapperProxy;
import org.apache.ibatis.session.SqlSession;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JpaMapperProxy extends MapperProxy {

    private final Map<Method, JpaMapperMethod> KnownJpaMethods = new ConcurrentHashMap<>();
    private final SqlSession jpaSqlSession;
    private final Class mapperInterface;
    private final List<JpaMapperMethodFactory> methodFactories;
    public final static JpaMapperMethod NULL = (Object proxy, Method method, Object[] args, SqlSession sqlSession)-> null;

    public JpaMapperProxy(SqlSession sqlSession, Class mapperInterface, Map methodCache, List<JpaMapperMethodFactory> methodFactories) {
        super(sqlSession, mapperInterface, methodCache);
        this.mapperInterface = mapperInterface;
        this.jpaSqlSession = sqlSession;
        this.methodFactories = methodFactories;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        JpaMapperMethod jpaMapperMethod = getCachedJpaMethod(method);
        return jpaMapperMethod!=NULL
                ? jpaMapperMethod.invoke(proxy, method, args, jpaSqlSession)
                : super.invoke(proxy, method, args);
    }

    private JpaMapperMethod getCachedJpaMethod(Method method){
        if(CollectionUtils.isEmpty(methodFactories)) return NULL;
        return KnownJpaMethods.computeIfAbsent(method, k -> {
            Value<JpaMapperMethod> value = new Value(NULL);
            methodFactories.stream().anyMatch(factory -> {
                if(factory.match(jpaSqlSession, mapperInterface, method)){
                    value.set(factory.newInstance(jpaSqlSession, mapperInterface, method));
                    return true;
                }
                return false;
            });
            return value.get();
        });
    }

}
