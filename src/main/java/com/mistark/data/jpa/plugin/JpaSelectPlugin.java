package com.mistark.data.jpa.plugin;

import com.mistark.data.jpa.helper.*;
import com.mistark.data.jpa.meta.EntityField;
import com.mistark.data.jpa.meta.EntityMeta;
import com.mistark.data.jpa.meta.Query;
import com.mistark.data.jpa.meta.Query.*;
import com.mistark.data.jpa.plugin.dialect.PageDialect;
import com.mistark.data.jpa.plugin.dialect.DialectHelper;
import com.mistark.data.jpa.support.TenantIdService;
import com.mistark.meta.Value;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class JpaSelectPlugin implements Interceptor {

    @Resource
    PluginConfig pluginConfig;

    private Map<Class, Object> getParams(BoundSql boundSql){
        Map<Class, Object> objectMap = new ConcurrentHashMap<>();
        Value<Consumer> callback = new Value<>();
        Set<Integer> set = new HashSet<>();
        callback.set(obj-> {
            if(set.contains(obj.hashCode())) return;
            set.add(obj.hashCode());
            if(Value.isArray(obj)) {
                Object[] objects = (Object[]) obj;
                for (int i=0;i<objects.length && i<10;i++){
                    if(objects[i] == null) continue;
                    objectMap.put(objects[i].getClass(), objects[i]);
                }
            }else if(obj!=null){
                objectMap.put(obj.getClass(), obj);
            }
        });
        Object parameterObject = boundSql.getParameterObject();
        if(parameterObject instanceof Map){
            ((Map)parameterObject).values().forEach(callback.get());
        } else {
            callback.get().accept(parameterObject);
        }
        return objectMap;
    }

    private SelectVars getVars(MappedStatement ms, BoundSql boundSql) throws Throwable {
        SelectVars vars = new SelectVars();
        Map<Class, Object> params = getParams(boundSql);
        Query query = null;
        Object pi = params.get(Query.class);
        if(pi instanceof Query){
            query = (Query) pi;
        }
        vars.ms = ms;
        vars.boundSql = boundSql;
        vars.params = params;
        vars.query = query;
        vars.markedSql = SqlHelper.getMarkedSql(boundSql);
        vars.plainSelect = SqlHelper.getPlainSelect(vars.markedSql);
        vars.plainSelect = SqlHelper.getPlainSelect(vars.markedSql);
        vars.columnMap = SqlHelper.getColumns(vars.plainSelect);
        vars.entityMeta = EntityHelper.fromMappedStatement(ms);
        vars.addonParams = new ConcurrentHashMap<>();
        return vars;
    }

    private Expression parseQueryContions(List<QueryCondition> conditions,SelectVars vars, boolean isSafeCheck) throws Throwable{
        if(CollectionUtils.isEmpty(conditions)) return null;
        Expression target = null;
        LogicOperator logicOperator = LogicOperator.AND;
        for (QueryCondition condition : conditions) {
            if(condition==null) continue;
            Expression patch;
            if(!CollectionUtils.isEmpty(condition.getConditions())){
                patch = parseQueryContions(condition.getConditions(), vars, isSafeCheck);
                patch = (patch == null) ? patch : new Parenthesis(patch);
            }else {
                patch = parseCondition(condition, vars, isSafeCheck);
            }
            if(patch == null) continue;
            target = target == null
                    ? patch
                    : logicOperator == LogicOperator.AND
                        ? new AndExpression(target, patch)
                        : new OrExpression(target, patch);
            logicOperator
                    = condition.getLogicOperator()!= null
                    ? condition.getLogicOperator()
                    : LogicOperator.AND;
        }
        return target;
    }
    
    private Expression parseCondition(QueryCondition condition, SelectVars vars, boolean isSafeCheck) throws Throwable {
        Value<String> field = new Value<>(condition.getField());
        if(field.get()!=null) field.set(field.get().trim());
        if(StringUtils.isEmpty(field.get())) return null;
        Value<String> operator = new Value<>(condition.getOperator());
        if(operator.get()!=null) operator.set(operator.get().trim());
        if(StringUtils.isEmpty(operator.get())) operator.set(Query.EQUAL);
        Class<? extends Expression> operatorType = QueryHelper.getExpression(operator.get());
        if(operatorType == null){
            throw new NotSupportedOperator(String.format("Unsupported operator %s", operator.get()));
        }
        EntityField fieldInfo = vars.entityMeta.getFields().get(field.get());
        Column column = vars.columnMap.get(field.get());
        if(column == null){
            if(fieldInfo!=null){
                column = new Column(fieldInfo.getColumn());
            }else if(!isSafeCheck){
                column = new Column(StringHelper.toUnderline(field.get()));
            }
            if(column == null) return null;
        }
        Expression target = operatorType.newInstance();
        Value<Class> valueJavaType = new Value<>(String.class);
        if(field!=null) valueJavaType.set(fieldInfo.getJavaType());
        Object value = condition.getValue();
        Map<String, Object> itemValues = new HashMap<>();
        if(!(target instanceof IsNullExpression) && value == null) {
            throw new InvalidParameter("value cannot be null");
        }
        if(target instanceof BinaryExpression){
            BinaryExpression binaryExpression = (BinaryExpression) target;
            if (binaryExpression instanceof LikeExpression) {
                if(Query.NOT_LIKE.equals(operator.get())){
                    ((LikeExpression) binaryExpression).setNot(true);
                }
                valueJavaType.set(String.class);
                value = QueryHelper.getLikeValue(operator.get(), value.toString());
            }
            String propKey = SqlHelper.getParamName();
            binaryExpression.setLeftExpression(column);
            binaryExpression.setRightExpression(SqlHelper.getMarkedExpression(propKey));
            itemValues.put(propKey, value);
        }else if(target instanceof InExpression || target instanceof Between){
            if(!value.getClass().isAssignableFrom(Collection.class) && !Value.isArray(value)){
                throw new InvalidParameter(String.format("operator \"%s\" requires an array", operator.get()));
            }
            Collection values = Value.isArray(value) ? Arrays.asList(value) : (Collection) value;
            if(target instanceof InExpression){
                InExpression in = (InExpression)target;
                in.setNot(Query.NOT_IN.equals(operator.get()));
                List<Expression> inValues = new ArrayList<>();
                values.forEach(v -> {
                    String k = SqlHelper.getParamName();
                    itemValues.put(k, v);
                    inValues.add(SqlHelper.getMarkedExpression(k));
                });
                in.setLeftExpression(column);
                in.setRightItemsList(new ExpressionList(inValues));
            }else if(target instanceof Between){
                if(values.size() < 2){
                    throw new InvalidParameter(String.format("operator \"%s\" requires two items", operator.get()));
                }
                Between between = (Between)target;
                String startKey = SqlHelper.getParamName();
                String endKey = SqlHelper.getParamName();
                between.setLeftExpression(column);
                between.setNot(Query.NOT_BETWEEN.equals(operator.get()));
                between.setBetweenExpressionStart(SqlHelper.getMarkedExpression(startKey));
                between.setBetweenExpressionEnd(SqlHelper.getMarkedExpression(endKey));
                Object[] btValues = values.toArray();
                itemValues.put(startKey, btValues[0]);
                itemValues.put(endKey, btValues[1]);
            }
        }else if(target instanceof IsNullExpression){
            IsNullExpression isNull = (IsNullExpression)target;
            isNull.setNot(Query.NOT_NULL.equals(operator.get()));
            isNull.setLeftExpression(column);
        }
        itemValues.entrySet().forEach(entry -> {
            Object val = entry.getValue();
            Class toType = valueJavaType.get();
            if(val!=null && toType != null && !toType.isAssignableFrom(val.getClass())){
                if(toType.isAssignableFrom(Date.class)){
                    String pattern =
                            fieldInfo!=null && fieldInfo.getPattern()!=null
                            ? fieldInfo.getPattern() : "yyyy-MM-dd HH:mm:ss";
                    DateConverter dateConverter = new DateConverter();
                    dateConverter.setPattern(pattern);
                    val = dateConverter.convert(toType, val);
                }else {
                    val = ConvertUtils.convert(val, toType);
                }
            }
            vars.addonParams.put(entry.getKey(), val);
        });
        return target;
    }

    private List<Table> getTables(PlainSelect plainSelect){
        List<Table> tables = new ArrayList<>();
        FromItem fromItem = plainSelect.getFromItem();
        if(fromItem instanceof Table){
            tables.add((Table) fromItem);
        }
        List<Join> joins = plainSelect.getJoins();
        if(!CollectionUtils.isEmpty(joins)) joins.forEach(join -> {
            FromItem item = join.getRightItem();
            if(item instanceof Table){
                tables.add((Table) item);
            }
        });
        return tables;
    }

    private Expression parseSoftDel(List<Table> tables, SelectVars vars){
        Expression target = null;
        for (Table table: tables){
            String name = table.getName();
            EntityMeta entityMeta = EntityHelper.resolve(name);
            if(!SoftDelHelper.isSoftDel(entityMeta)) continue;
            Column column = new Column();
            column.setTable(table);
            column.setColumnName(entityMeta.getSoftDel().getColumn());
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(column);
            String key = SqlHelper.getParamName();
            vars.addonParams.put(key, SoftDelHelper.getValue(false, entityMeta.getSoftDel().getJavaType()));
            equalsTo.setRightExpression(SqlHelper.getMarkedExpression(key));
            target = target == null ? equalsTo : new AndExpression(target, equalsTo);
        }
        return target;
    }

    private Expression parseTenant(List<Table> tables, SelectVars vars){
        TenantIdService tenantIdService = pluginConfig.getTenantIdService();
        if(tenantIdService==null) return null;
        Expression target = null;
        for (Table table: tables){
            String name = table.getName();
            EntityMeta entityMeta = EntityHelper.resolve(name);
            if(entityMeta==null || entityMeta.getTenantId()==null) continue;
            Column column = new Column();
            column.setTable(table);
            column.setColumnName(entityMeta.getTenantId().getColumn());
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(column);
            String key = SqlHelper.getParamName();
            vars.addonParams.put(key, tenantIdService.getTenantId());
            equalsTo.setRightExpression(SqlHelper.getMarkedExpression(key));
            target = target == null ? equalsTo : new AndExpression(target, equalsTo);
        }
        return target;
    }

    void patchWhere(SelectVars vars) throws Throwable {
        PlainSelect plainSelect = vars.plainSelect;
        List<Expression> conditions = new ArrayList<>();
        conditions.add(plainSelect.getWhere());
        Query query = vars.query;
        if(query!=null){
            conditions.add(parseQueryContions(query.getConditions(), vars, query.isSafeCheck()));
        }
        List<Table> tables = getTables(vars.plainSelect);
        conditions.add(parseSoftDel(tables, vars));
        conditions.add(parseTenant(tables, vars));
        Expression target = plainSelect.getWhere();
        for (Expression exp:conditions){
            if(exp==null) continue;
            exp = new Parenthesis(exp);
            target = target==null ? exp : new AndExpression(target, exp);
        }
        plainSelect.setWhere(target);
    }

    private void patchTotal(SelectVars vars){
        EntityMeta meta = vars.entityMeta;
        PlainSelect plainSelect = vars.plainSelect;
        plainSelect.setSelectItems(new ArrayList<SelectItem>(){{
            SelectExpressionItem selectItem = new SelectExpressionItem();
            selectItem.setExpression(new Column("COUNT(1)"));
            selectItem.setAlias(new Alias(meta.getId().getName()));
            add(selectItem);
        }});
        plainSelect.setLimit(null);
        plainSelect.setOrderByElements(null);
    }

    private void patchPage(SelectVars vars) throws Throwable {
        DataSource dataSource = vars.ms.getConfiguration().getEnvironment().getDataSource();
        PageDialect pageDialect = DialectHelper.getPageDialect(dataSource);
        Value<PlainSelect> plainSelectValue = new Value<>(vars.plainSelect);
        pageDialect.patch(vars.query.getPage(), vars.query.getPageSize(), plainSelectValue, vars.addonParams);
    }

    private void updateBoundSql(SelectVars vars){
        Map<String, ParameterMapping> pre = new HashMap<>();
        PlainSelect plainSelect = vars.plainSelect;
        BoundSql boundSql = vars.boundSql;
        boundSql.getParameterMappings().forEach(i -> pre.put(i.getProperty(), i));
        String sql = plainSelect.toString();
        List<ParameterMapping> parameterMappings = new ArrayList<>();
        SqlHelper.forEachMarkedField(sql, prop -> {
            ParameterMapping parameterMapping = pre.get(prop);
            if(parameterMapping==null){
                Object value = vars.addonParams.get(prop);
                boundSql.setAdditionalParameter(prop, value);
                Class valueType = value!=null ? value.getClass() : Object.class;
                parameterMapping = new ParameterMapping.Builder(vars.ms.getConfiguration(), prop, valueType).build();
            }
            parameterMappings.add(parameterMapping);
        });
        sql = SqlHelper.getUnMarkedSql(sql);
        MetaObject metaObject = SystemMetaObject.forObject(boundSql);
        metaObject.setValue("sql", sql);
        metaObject.setValue("parameterMappings", parameterMappings);
    }


    private void patch(MappedStatement ms, BoundSql boundSql) throws Throwable {
        SelectVars vars = getVars(ms, boundSql);
        patchWhere(vars);
        Query query = vars.query;
        if(query!=null) {
            QueryType queryType = query.getQueryType();
            if(queryType == QueryType.TOTAL){
                patchTotal(vars);
            }else if(queryType == QueryType.PAGE){
                patchPage(vars);
            }
        }
        updateBoundSql(vars);
        System.out.println(vars.boundSql.getSql());
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MappedStatement ms = PluginHelper.getMappedStatement(statementHandler);
        BoundSql boundSql = statementHandler.getBoundSql();
        if(ms.getSqlCommandType() == SqlCommandType.SELECT
                || ms.getStatementType()!= StatementType.CALLABLE){
            patch(ms, boundSql);
        }
        return invocation.proceed();
    }

    private class SelectVars {
        private Map<Class, Object> params;
        private MappedStatement ms;
        private BoundSql boundSql;
        private Query query;
        private String markedSql;
        private PlainSelect plainSelect;
        private Map<String, Column> columnMap;
        private EntityMeta entityMeta;
        private Map<String, Object> addonParams;
    }

}
