package com.mistark.data.jpa.plugin;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.stereotype.Component;

@Component
@Order(PluginOrders.DELETE)
public class JpaDeletePlugin implements JpaPlugin {

    @Override
    public boolean match(MappedStatement ms, BoundSql boundSql) {
        return ms.getSqlCommandType() == SqlCommandType.DELETE;
    }

    @Override
    public void patch(MappedStatement ms, BoundSql boundSql) throws Throwable {

    }
}
