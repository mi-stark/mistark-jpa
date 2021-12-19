package com.mistark.data.jpa.plugin;

import com.mistark.data.jpa.helper.EntityHelper;
import com.mistark.data.jpa.helper.PluginHelper;
import com.mistark.data.jpa.meta.EntityMeta;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.delete.Delete;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(PluginOrders.DELETE)
public class JpaDeletePlugin implements JpaPlugin {
    @Resource
    PluginConfig pluginConfig;

    private DeleteVars getVars(MappedStatement ms, BoundSql boundSql) throws Throwable {
        DeleteVars vars = new DeleteVars();
        vars.ms = ms;
        vars.boundSql = boundSql;
        vars.meta = EntityHelper.fromStatement(ms);
        vars.delete = PluginHelper.getDelete(PluginHelper.getMarkedSql(boundSql));
        vars.addon = new HashMap<>();
        return vars;
    }

    private void patchWhere(DeleteVars vars){
        Delete delete = vars.delete;
        List<Expression> whereItems = new ArrayList<>();
        whereItems.add(delete.getWhere());
        whereItems.add(PluginHelper.parseTenant(vars.meta, vars.addon, pluginConfig));
        delete.setWhere(PluginHelper.mergeWhere(whereItems));
    }

    @Override
    public boolean match(MappedStatement ms, BoundSql boundSql) {
        return ms.getSqlCommandType() == SqlCommandType.DELETE;
    }

    @Override
    public void patch(MappedStatement ms, BoundSql boundSql) throws Throwable {
        DeleteVars vars = getVars(ms,boundSql);
        if(vars.meta==null) return;
        patchWhere(vars);
        PluginHelper.updateBoundSql(vars.ms, vars.boundSql, vars.delete.toString(), vars.addon);
    }

    private static class DeleteVars{
        private MappedStatement ms;
        private BoundSql boundSql;
        private EntityMeta meta;
        private Delete delete;
        private Map<String, Object> addon;
    }
}
