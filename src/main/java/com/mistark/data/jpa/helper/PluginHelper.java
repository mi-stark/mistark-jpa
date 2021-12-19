package com.mistark.data.jpa.helper;

import com.mistark.data.jpa.annotation.SoftDel;
import com.mistark.data.jpa.annotation.TenantId;
import com.mistark.data.jpa.annotation.Version;
import com.mistark.data.jpa.meta.EntityMeta;
import com.mistark.data.jpa.plugin.JpaUpdatePlugin;
import com.mistark.data.jpa.plugin.PluginConfig;
import com.mistark.meta.Value;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginHelper {
    
    private final static String FIELD_MARK_TPL = "\"#_{%s}_#\"";
    private final static String FIELD_MARK_REG = "\"#_\\{([\\S]+)\\}_#\"";
    private final static AtomicLong seed = new AtomicLong(0);

    public static PlainSelect getPlainSelect(String sql) throws Throwable {
        Statement statement = CCJSqlParserUtil.parse(sql);
        return (PlainSelect)((Select)statement).getSelectBody();
    }

    public static Update getUpdate(String sql) throws Throwable {
        Statement statement = CCJSqlParserUtil.parse(sql);
        return (Update) statement;
    }

    public static Delete getDelete(String sql) throws Throwable {
        Statement statement = CCJSqlParserUtil.parse(sql);
        return (Delete) statement;
    }
    
    

    public static Map<String, Column> getColumns(PlainSelect plainSelect) {
        Map<String, Column> columns = new ConcurrentHashMap<>();
        List<SelectItem> selectItemList = plainSelect.getSelectItems();
        if(CollectionUtils.isEmpty(selectItemList)) return columns;
        selectItemList.forEach(selectItem -> {
            SelectExpressionItem selectExpressionItem = (SelectExpressionItem) selectItem;
            Expression expression = selectExpressionItem.getExpression();
            if(!(expression instanceof Column)) return;
            Column column = (Column) expression;
            Alias alias = selectExpressionItem.getAlias();
            String field = alias!=null ? alias.getName() : column.getColumnName();
            columns.put(field, column);
        });
        return columns;
    }

    public static Map<String, Expression> getUpdateSets(List<UpdateSet> updateSets){
        Map<String, Expression> expressionMap = new HashMap<>();
        updateSets.stream().forEach(updateSet -> {
            Column column =  updateSet.getColumns().get(0);
            Expression expression =  updateSet.getExpressions().get(0);
            expressionMap.put(column.getColumnName(), expression);
        });
        return expressionMap;
    }

    public static Expression mergeWhere(List<Expression> whereItems){
        Expression target = null;
        for (Expression exp : whereItems){
            if(exp==null) continue;
            exp = new Parenthesis(exp);
            target = target==null ? exp : new AndExpression(target, exp);
        }
        return target;
    }

    public static String getMarkedName(String name){
        return String.format(FIELD_MARK_TPL, name);
    }

    public static Expression getMarkedExpression(String name){
        return new Column(getMarkedName(name));
    }

    public static String getParamName(){
        return String.format("_sp_%s", seed.addAndGet(1L));
    }

    public static String getMarkedSql(BoundSql boundSql){
        Collection<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if(CollectionUtils.isEmpty(parameterMappings)) return boundSql.getSql();
        Value<String> sql = new Value<>(boundSql.getSql());
        parameterMappings.forEach(i -> sql.set(sql.get().replaceFirst("\\?", getMarkedName(i.getProperty()))));
        return sql.get();
    }

    public static String getUnMarkedSql(String sql){
        return sql.replaceAll(FIELD_MARK_REG, "?");
    }

    public static void forEachMarkedField(String sql, Consumer<String> consumer){
        Pattern pattern = Pattern.compile(FIELD_MARK_REG);
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) consumer.accept(matcher.group(1));
    }
    
    public static MappedStatement getMappedStatement(StatementHandler statementHandler) {
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        return (MappedStatement) metaObject.getValue("delegate.mappedStatement");
    }

    public static EqualsTo parseTenant(EntityMeta meta, Map<String, Object> addon, PluginConfig config){
        if(!config.hasTenant() || !meta.hasAnnoField(TenantId.class)) return null;
        String key = getParamName();
        EqualsTo equalsTo = new EqualsTo();
        Column column = new Column(meta.annoFieldColumn(TenantId.class));
        equalsTo.setLeftExpression(column);
        equalsTo.setRightExpression(PluginHelper.getMarkedExpression(key));
        addon.put(key, config.getTenantId());
        return equalsTo;
    }

    public static EqualsTo parseSoftDel(EntityMeta meta, Map<String, Object> addon){
        if(!meta.isSoftDel()) return null;
        String key = PluginHelper.getParamName();
        EqualsTo equalsTo = new EqualsTo();
        Column column = new Column(meta.annoFieldColumn(SoftDel.class));
        equalsTo.setLeftExpression(column);
        equalsTo.setRightExpression(PluginHelper.getMarkedExpression(key));
        addon.put(key, SoftDelHelper.getValue(false, meta));
        return equalsTo;
    }
    
    public static void updateBoundSql(MappedStatement ms, 
                                      BoundSql boundSql, 
                                      String sql,
                                      Map<String, Object> addon){
        Map<String, ParameterMapping> pre = new HashMap<>();
        boundSql.getParameterMappings().forEach(i -> pre.put(i.getProperty(), i));
        List<ParameterMapping> parameterMappings = new ArrayList<>();
        forEachMarkedField(sql, prop -> {
            ParameterMapping parameterMapping = pre.get(prop);
            if(parameterMapping==null){
                Object value = addon.get(prop);
                boundSql.setAdditionalParameter(prop, value);
                Class valueType = value!=null ? value.getClass() : Object.class;
                parameterMapping = new ParameterMapping.Builder(ms.getConfiguration(), prop, valueType).build();
            }
            parameterMappings.add(parameterMapping);
        });
        sql = getUnMarkedSql(sql);
        MetaObject metaObject = SystemMetaObject.forObject(boundSql);
        metaObject.setValue("sql", sql);
        metaObject.setValue("parameterMappings", parameterMappings);
    }

}
