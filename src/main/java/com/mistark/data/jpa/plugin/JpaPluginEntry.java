package com.mistark.data.jpa.plugin;

import com.mistark.data.jpa.helper.PluginHelper;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Arrays;

@Component
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class JpaPluginEntry implements Interceptor {

    private JpaPlugin[] jpaPlugins;

    public JpaPluginEntry(ObjectProvider<JpaPlugin[]> jpaPluginsProvider) {
        JpaPlugin[] jpaPlugins = jpaPluginsProvider.getIfAvailable();
        if(jpaPlugins!=null) {
            Arrays.sort(jpaPlugins, (JpaPlugin jpaPlugin1, JpaPlugin jpaPlugin2)->{
                Order order_1 = AnnotationUtils.getAnnotation(jpaPlugin1.getClass(), Order.class);
                Order order_2 = AnnotationUtils.getAnnotation(jpaPlugin2.getClass(), Order.class);
                int order1 = order_1 == null ? 0 : order_1.value();
                int order2 = order_2 == null ? 0 : order_2.value();
                return order1 - order2;
            });
        }
        this.jpaPlugins = jpaPlugins;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MappedStatement ms = PluginHelper.getMappedStatement(statementHandler);
        BoundSql boundSql = statementHandler.getBoundSql();
        if(jpaPlugins!=null && jpaPlugins.length>0){
            for (JpaPlugin plugin: jpaPlugins) {
                if(!plugin.match(ms, boundSql)) continue;
                plugin.patch(ms, boundSql);
            }
        }
        return invocation.proceed();
    }
}
