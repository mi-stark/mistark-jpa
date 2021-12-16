package com.mistark.data.jpa.plugin;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.stereotype.Component;

@Component
@Order(PluginOrder.UPDATE)
public class JpaUpdatePlugin implements JpaPlugin {

    private UpdateVars getVars(MappedStatement ms, BoundSql boundSql){
        UpdateVars vars = new UpdateVars();
        vars.ms = ms;
        vars.boundSql = boundSql;
        vars.param = boundSql.getParameterObject();
        return vars;
    }

    @Override
    public boolean match(MappedStatement ms, BoundSql boundSql) {
        return ms.getSqlCommandType() == SqlCommandType.UPDATE;
    }

    @Override
    public void patch(MappedStatement ms, BoundSql boundSql){
        UpdateVars vars = getVars(ms, boundSql);


    }

    private class UpdateVars{
        private MappedStatement ms;
        private BoundSql boundSql;
        private Object param;
    }
}
