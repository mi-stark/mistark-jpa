package com.mistark.data.jpa.plugin;

import com.mistark.data.jpa.helper.ConvertHelper;
import com.mistark.data.jpa.helper.EntityHelper;
import com.mistark.data.jpa.helper.PluginHelper;
import com.mistark.data.jpa.helper.SoftDelHelper;
import com.mistark.data.jpa.meta.EntityMeta;
import com.mistark.data.jpa.support.TenantIdService;
import com.mistark.meta.time.Clock;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
@Order(PluginOrders.UPDATE)
public class JpaUpdatePlugin implements JpaPlugin {

    @Resource
    PluginConfig pluginConfig;

    private UpdateVars getVars(MappedStatement ms, BoundSql boundSql) throws Throwable {
        UpdateVars vars = new UpdateVars();
        vars.ms = ms;
        vars.boundSql = boundSql;
        vars.param = boundSql.getParameterObject();
        vars.addon = new HashMap<>();
        vars.meta = EntityHelper.fromStatement(ms);
        try {
            vars.update = PluginHelper.getUpdate(PluginHelper.getMarkedSql(boundSql));
        }catch (Throwable e){}
        vars.metaObject = SystemMetaObject.forObject(vars.param);
        return vars;
    }

    private Expression parseSoftDel(UpdateVars vars){
        if(!vars.meta.isSoftDel()) return null;
        String key = PluginHelper.getParamName();
        EqualsTo equalsTo = new EqualsTo();
        Column column = new Column(vars.meta.getSoftDel().getColumn());
        equalsTo.setLeftExpression(column);
        equalsTo.setRightExpression(PluginHelper.getMarkedExpression(key));
        vars.addon.put(key, SoftDelHelper.getValue(false, vars.meta));
        return equalsTo;
    }

    private Expression parseTenant(UpdateVars vars){
        TenantIdService tenantIdService = pluginConfig.getTenantIdService();
        if(tenantIdService==null || vars.meta.getTenantId()==null) return null;
        String key = PluginHelper.getParamName();
        EqualsTo equalsTo = new EqualsTo();
        Column column = new Column(vars.meta.getTenantId().getColumn());
        equalsTo.setLeftExpression(column);
        equalsTo.setRightExpression(PluginHelper.getMarkedExpression(key));
        vars.addon.put(key, tenantIdService.getTenantId());
        return equalsTo;
    }

    private Expression parseVersion(UpdateVars vars){
        if(vars.meta.getVersion()==null) return null;
        String key = PluginHelper.getParamName();
        Object oldVersion = vars.metaObject.getValue(vars.meta.getVersion().getName());
        EqualsTo equalsTo = new EqualsTo();
        Column column = new Column(vars.meta.getVersion().getColumn());
        equalsTo.setLeftExpression(column);
        equalsTo.setRightExpression(PluginHelper.getMarkedExpression(key));
        vars.addon.put(key, oldVersion);
        return equalsTo;
    }

    private void patchWhere(UpdateVars vars){
        Update update = vars.update;
        List<Expression> whereItems = new ArrayList<>();
        whereItems.add(vars.update.getWhere());
        whereItems.add(parseSoftDel(vars));
        whereItems.add(parseTenant(vars));
        whereItems.add(parseVersion(vars));
        update.setWhere(PluginHelper.mergeWhereItems(whereItems));
    }

    private void patchParam(UpdateVars vars){
        if(!vars.param.getClass().equals(vars.meta.getEntity())
                && !(vars.param instanceof Map)){
            return;
        }
        Update update = vars.update;
        EntityMeta meta = vars.meta;
        MetaObject metaObject = vars.metaObject;
        Map<EntityMeta.EntityField, Object> metaValue = new HashMap<>();

        Date now = Clock.currentDate();
        if(meta.getUpdateDate()!=null){
            metaValue.put(meta.getUpdateDate(), now);
        }

        if(meta.getVersion()!=null){
            Number old = (Number)metaObject.getValue(meta.getVersion().getName());
            Object version = old == null ? now.getTime() : old.longValue() + 1;
            metaValue.put(meta.getVersion(), version);
        }

        if(pluginConfig.getUserIdService()!=null){
            Object userId = pluginConfig.getUserIdService().getUserId();
            if(meta.getUpdateBy()!=null){
                metaValue.put(meta.getUpdateBy(), userId);
            }
        }
        List<UpdateSet> updateSets = update.getUpdateSets();
        Map<String, Expression> updateSetMap = PluginHelper.getUpdateSets(updateSets);
        metaValue.entrySet().forEach( entry -> {
            EntityMeta.EntityField field = entry.getKey();
            Object target = ConvertHelper.convert(entry.getValue(),field.getJavaType(), field.getPattern());
            if(updateSetMap.containsKey(field.getName())){
                metaObject.setValue(entry.getKey().getName(), target);
            }else {
                String key = field.getName();
                Column column = new Column(field.getColumn());
                Expression setExpression = PluginHelper.getMarkedExpression(key);
                UpdateSet updateSet = new UpdateSet(column, setExpression);
                updateSets.add(updateSet);
                vars.addon.put(field.getName(), target);
            }
        });
    }

    @Override
    public boolean match(MappedStatement ms, BoundSql boundSql) {
        return ms.getSqlCommandType() == SqlCommandType.UPDATE && !SoftDelHelper.isSoftDel(ms);
    }

    @Override
    public void patch(MappedStatement ms, BoundSql boundSql) throws Throwable {
        UpdateVars vars = getVars(ms, boundSql);
        if(vars.meta==null || vars.update==null) return;
        patchWhere(vars);
        patchParam(vars);
        PluginHelper.updateBoundSql(vars.ms, vars.boundSql, vars.update.toString(), vars.addon);
    }

    private class UpdateVars{
        private MappedStatement ms;
        private BoundSql boundSql;
        private Object param;
        private EntityMeta meta;
        private Update update;
        private Map<String, Object> addon;
        private MetaObject metaObject;
    }
}
