package com.mistark.data.jpa.plugin;

import com.mistark.data.jpa.annotation.*;
import com.mistark.data.jpa.helper.ConvertHelper;
import com.mistark.data.jpa.helper.EntityHelper;
import com.mistark.data.jpa.helper.PluginHelper;
import com.mistark.data.jpa.helper.SoftDelHelper;
import com.mistark.data.jpa.meta.EntityMeta;
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
        vars.isSoftDel = SoftDelHelper.isSoftDel(ms);
        try {
            vars.update = PluginHelper.getUpdate(PluginHelper.getMarkedSql(boundSql));
        }catch (Throwable e){}
        vars.metaObject = SystemMetaObject.forObject(vars.param);
        return vars;
    }

    private Expression parseVersion(UpdateVars vars){
        if(!vars.meta.hasAnnoField(Version.class)) return null;
        String key = PluginHelper.getParamName();
        Object oldVersion = vars.metaObject.getValue(vars.meta.annoFieldName(Version.class));
        EqualsTo equalsTo = new EqualsTo();
        Column column = new Column(vars.meta.annoFieldColumn(Version.class));
        equalsTo.setLeftExpression(column);
        equalsTo.setRightExpression(PluginHelper.getMarkedExpression(key));
        vars.addon.put(key, oldVersion);
        return equalsTo;
    }

    private void patchWhere(UpdateVars vars){
        Update update = vars.update;
        List<Expression> whereItems = new ArrayList<>();
        whereItems.add(update.getWhere());
        whereItems.add(PluginHelper.parseSoftDel(vars.meta, vars.addon));
        whereItems.add(PluginHelper.parseTenant(vars.meta, vars.addon, pluginConfig));
        if(!vars.isSoftDel){
            whereItems.add(parseVersion(vars));
        }
        update.setWhere(PluginHelper.mergeWhere(whereItems));
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
        if(meta.hasAnnoField(UpdateDate.class)){
            metaValue.put(meta.annoField(UpdateDate.class), now);
        }

        if(meta.hasAnnoField(Version.class)){
            Number old = (Number)metaObject.getValue(meta.annoFieldName(Version.class));
            Object version = old == null ? now.getTime() : old.longValue() + 1;
            metaValue.put(meta.annoField(Version.class), version);
        }

        if(pluginConfig.hasUser()){
            Object userId = pluginConfig.getUserId();
            if(meta.hasAnnoField(UpdateBy.class)){
                metaValue.put(meta.annoField(UpdateBy.class), userId);
            }
        }
        
        if(vars.isSoftDel && meta.hasAnnoField(SoftDel.class)){
            metaValue.put(meta.annoField(SoftDel.class), SoftDelHelper.getValue(true, meta));
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
        return ms.getSqlCommandType() == SqlCommandType.UPDATE;
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
        private boolean isSoftDel;
    }
}
