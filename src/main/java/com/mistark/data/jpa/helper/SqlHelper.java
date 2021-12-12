package com.mistark.data.jpa.helper;

import com.mistark.meta.Value;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlHelper {

    private final static String FIELD_MARK_TPL = "\"#_{%s}_#\"";
    private final static String FIELD_MARK_REG = "\"#_\\{([\\S]+)\\}_#\"";
    private static long seed = 0;

    public static PlainSelect getPlainSelect(String sql) throws JSQLParserException {
        Statement statement = CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect)((Select)statement).getSelectBody();
        return plainSelect;
    }

    public static Map<String, Column> getColumns(PlainSelect plainSelect) {
        Map<String, Column> columns = new HashMap<>();
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

    public static String getMarkedName(String name){
        return String.format(FIELD_MARK_TPL, name);
    }
    
    public static Expression getMarkedExpression(String name){
        return new Column(getMarkedName(name));
    }

    public static String getParamName(){
        return String.format("_sp_%s", ++seed);
    }

    public static String getMarkedSql(BoundSql boundSql){
        Value<String> sql = new Value<>(boundSql.getSql());
        Collection<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if(CollectionUtils.isEmpty(parameterMappings)) return sql.get();
        parameterMappings.forEach(i -> sql.set(sql.get().replaceFirst("\\?", getMarkedName(i.getProperty()))));
        return sql.get();
    }

    public static String getUnMarkedSql(String sql){
        return sql.replaceAll(FIELD_MARK_REG, "?");
    }

    public static Iterator<String> getMarkedIterator(String sql){
        Pattern pattern = Pattern.compile(FIELD_MARK_REG);
        Matcher matcher = pattern.matcher(sql);
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return matcher.find();
            }

            @Override
            public String next() {
                return matcher.group(1);
            }
        };
    }
}
