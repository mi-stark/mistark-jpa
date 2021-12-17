package com.mistark.data.jpa.plugin;

import com.mistark.data.jpa.annotation.SortType;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Order(PluginOrder.SELECT)
public class JpaSelectPlugin implements JpaPlugin {

    @Resource
    PluginConfig pluginConfig;

    private Map<Class, Object> getParams(BoundSql boundSql){
        Map<Class, Object> objectMap = new HashMap<>();
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
        vars.sql = PluginHelper.getMarkedSql(boundSql);
        vars.select = PluginHelper.getPlainSelect(vars.sql);
        vars.select = PluginHelper.getPlainSelect(vars.sql);
        vars.columns = PluginHelper.getColumns(vars.select);
        vars.meta = EntityHelper.fromStatement(ms);
        vars.addon = new HashMap<>();
        return vars;
    }

    private Expression parseQueryFilters(List<QueryFilter> filters,SelectVars vars, boolean isSafeCheck) throws Throwable{
        if(CollectionUtils.isEmpty(filters)) return null;
        Expression target = null;
        LogicOperator logicOperator = LogicOperator.AND;
        for (QueryFilter filter : filters) {
            if(filter==null) continue;
            Expression patch;
            if(!CollectionUtils.isEmpty(filter.getGroup())){
                patch = parseQueryFilters(filter.getGroup(), vars, isSafeCheck);
                patch = (patch == null) ? patch : new Parenthesis(patch);
            }else {
                patch = parseFilter(filter, vars, isSafeCheck);
            }
            if(patch == null) continue;
            target = target == null
                    ? patch
                    : logicOperator == LogicOperator.AND
                        ? new AndExpression(target, patch)
                        : new OrExpression(target, patch);
            logicOperator
                    = filter.getLogicOperator()!= null
                    ? filter.getLogicOperator()
                    : LogicOperator.AND;
        }
        return target;
    }
    
    private Expression parseFilter(QueryFilter filter, SelectVars vars, boolean isSafeCheck) throws Throwable {
        Value<String> field = new Value<>(filter.getField());
        if(field.get()!=null) field.set(field.get().trim());
        if(StringUtils.isEmpty(field.get())) return null;
        Value<String> operator = new Value<>(filter.getOperator());
        if(operator.get()!=null) operator.set(operator.get().trim());
        if(StringUtils.isEmpty(operator.get())) operator.set(Query.EQUAL);
        Class<? extends Expression> operatorType = QueryHelper.getExpression(operator.get());
        if(operatorType == null){
            throw new NotSupportedOperator(String.format("Unsupported operator %s", operator.get()));
        }
        EntityField fieldInfo = vars.meta.getFields().get(field.get());
        Column column = vars.columns.get(field.get());
        if(column == null){
            if(fieldInfo!=null){
                column = new Column(fieldInfo.getColumn());
            }else if(!isSafeCheck){
                column = new Column(StringHelper.toUnderline(field.get()));
            }
            if(column == null) return null;
        }
        Expression target = operatorType.newInstance();
        Value<Class> targetType = new Value<>(String.class);
        if(fieldInfo!=null) targetType.set(fieldInfo.getJavaType());
        Object value = filter.getValue();
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
                targetType.set(String.class);
                value = QueryHelper.getLikeValue(operator.get(), value.toString());
            }
            String propKey = PluginHelper.getParamName();
            binaryExpression.setLeftExpression(column);
            binaryExpression.setRightExpression(PluginHelper.getMarkedExpression(propKey));
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
                    String k = PluginHelper.getParamName();
                    itemValues.put(k, v);
                    inValues.add(PluginHelper.getMarkedExpression(k));
                });
                in.setLeftExpression(column);
                in.setRightItemsList(new ExpressionList(inValues));
            }else if(target instanceof Between){
                if(values.size() < 2){
                    throw new InvalidParameter(String.format("operator \"%s\" requires two items", operator.get()));
                }
                Between between = (Between)target;
                String startKey = PluginHelper.getParamName();
                String endKey = PluginHelper.getParamName();
                between.setLeftExpression(column);
                between.setNot(Query.NOT_BETWEEN.equals(operator.get()));
                between.setBetweenExpressionStart(PluginHelper.getMarkedExpression(startKey));
                between.setBetweenExpressionEnd(PluginHelper.getMarkedExpression(endKey));
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
            Class toType = targetType.get();
            if(val!=null && toType != null && !toType.isAssignableFrom(val.getClass())){
                ConvertHelper.convert(val, toType, fieldInfo!=null ? fieldInfo.getPattern() : null);
            }
            vars.addon.put(entry.getKey(), val);
        });
        return target;
    }

    private List<Table> getTables(PlainSelect select){
        List<Table> tables = new ArrayList<>();
        FromItem fromItem = select.getFromItem();
        if(fromItem instanceof Table){
            tables.add((Table) fromItem);
        }
        List<Join> joins = select.getJoins();
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
            EntityMeta meta = EntityHelper.resolve(name);
            if(!SoftDelHelper.isSoftDel(meta)) continue;
            Column column = new Column();
            column.setTable(table);
            column.setColumnName(meta.getSoftDel().getColumn());
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(column);
            String key = PluginHelper.getParamName();
            vars.addon.put(key, SoftDelHelper.getValue(false, meta));
            equalsTo.setRightExpression(PluginHelper.getMarkedExpression(key));
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
            EntityMeta meta = EntityHelper.resolve(name);
            if(meta==null || meta.getTenantId()==null) continue;
            Column column = new Column();
            column.setTable(table);
            column.setColumnName(meta.getTenantId().getColumn());
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(column);
            String key = PluginHelper.getParamName();
            vars.addon.put(key, tenantIdService.getTenantId());
            equalsTo.setRightExpression(PluginHelper.getMarkedExpression(key));
            target = target == null ? equalsTo : new AndExpression(target, equalsTo);
        }
        return target;
    }

    void patchWhere(SelectVars vars) throws Throwable {
        PlainSelect select = vars.select;
        List<Expression> whereItems = new ArrayList<>();
        whereItems.add(select.getWhere());
        Query query = vars.query;
        if(query!=null){
            whereItems.add(parseQueryFilters(query.getFilters(), vars, query.isSafeCheck()));
        }
        List<Table> tables = getTables(vars.select);
        whereItems.add(parseSoftDel(tables, vars));
        whereItems.add(parseTenant(tables, vars));
        Expression target = select.getWhere();
        for (Expression exp : whereItems){
            if(exp==null) continue;
            exp = new Parenthesis(exp);
            target = target==null ? exp : new AndExpression(target, exp);
        }
        select.setWhere(target);
    }

    private void patchTotal(SelectVars vars){
        EntityMeta meta = vars.meta;
        PlainSelect select = vars.select;
        select.setSelectItems(new ArrayList<SelectItem>(){{
            SelectExpressionItem selectItem = new SelectExpressionItem();
            selectItem.setExpression(new Column("COUNT(1)"));
            selectItem.setAlias(new Alias(meta.getId().getName()));
            add(selectItem);
        }});
        select.setLimit(null);
        select.setOrderByElements(null);
    }

    private void patchOrderBy(SelectVars vars){
        Query query = vars.query;
        if(query==null || CollectionUtils.isEmpty(query.getSorters())) return;
        Map<String, OrderByElement> sortMap = new HashMap<>();
        List<OrderByElement> orderByElements = vars.select.getOrderByElements();
        if(!CollectionUtils.isEmpty(orderByElements)) {
            orderByElements.forEach(odby -> sortMap.put(odby.getExpression().toString(), odby));
        }
        query.getSorters().forEach(querySort -> {
            Column column = vars.columns.get(querySort.getOrderBy());
            if(column==null) return;
            OrderByElement orderByElement = new OrderByElement();
            orderByElement.setExpression(column);
            orderByElement.setAsc(querySort.getSortType() == SortType.ASC);
            sortMap.put(column.toString(), orderByElement);
        });
        orderByElements = sortMap.values().stream().collect(Collectors.toList());
        vars.select.setOrderByElements(orderByElements);
    }

    private void patchLimit(SelectVars vars) throws Throwable {
        DataSource dataSource = vars.ms.getConfiguration().getEnvironment().getDataSource();
        PageDialect pageDialect = DialectHelper.getPageDialect(dataSource);
        Value<PlainSelect> selectValue = new Value<>(vars.select);
        pageDialect.patch(vars.query.getPage(), vars.query.getPageSize(), selectValue, vars.addon);
        vars.select = selectValue.get();
    }

    @Override
    public boolean match(MappedStatement ms, BoundSql boundSql) {
        return ms.getSqlCommandType() == SqlCommandType.SELECT
                && ms.getStatementType()!=StatementType.CALLABLE;
    }

    @Override
    public void patch(MappedStatement ms, BoundSql boundSql) throws Throwable {
        SelectVars vars = getVars(ms, boundSql);
        patchWhere(vars);
        Query query = vars.query;
        if(query!=null) {
            QueryType queryType = query.getQueryType();
            if(queryType == QueryType.TOTAL){
                patchTotal(vars);
            }else {
                patchOrderBy(vars);
                if(queryType == QueryType.PAGE){
                    patchLimit(vars);
                }
            }
        }
        PluginHelper.updateBoundSql(vars.ms, vars.boundSql, vars.select.toString(), vars.addon);
    }

    private class SelectVars {
        private MappedStatement ms;
        private BoundSql boundSql;
        private Map<Class, Object> params;
        private Query query;
        private String sql;
        private PlainSelect select;
        private Map<String, Column> columns;
        private EntityMeta meta;
        private Map<String, Object> addon;
    }

}
