package com.mistark.data.jpa.helper;

import com.mistark.data.jpa.annotation.Id;
import com.mistark.data.jpa.annotation.SoftDel;
import com.mistark.data.jpa.annotation.SortType;
import com.mistark.data.jpa.meta.EntityMeta;
import com.mistark.data.jpa.meta.EntityMeta.*;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ParserHelper {

    public static String getSelectItems(EntityMeta meta){
        return meta.fields()
                .stream()
                .map(f-> String.format("%s.%s AS %s",
                        f.getTableAlias(),
                        f.getColumn(),
                        f.getName()))
                .collect(Collectors.joining(","));
    }

    public static String getJoinItems(EntityMeta meta){
        if(CollectionUtils.isEmpty(meta.getJoins())) return "";
        return meta.getJoins()
                .stream()
                .map(j -> String.format(
                            "%s JOIN %s %s ON ",
                            j.getJoinType(),
                            j.getTable(),
                            j.getAlias(),
                            j.getOn()))
                .collect(Collectors.joining(" "));
    }

    public static String getOrderByItems(EntityMeta meta){
        if(CollectionUtils.isEmpty(meta.getOrderBys())) return "";
        List<OrderByElement> orderByElements = meta.getOrderBys()
                .stream()
                .map(j -> {
                    EntityField field = meta.resolve(j.getField());
                    Column column = new Column(field.getTableAlias()+ "."+ field.getColumn());
                    OrderByElement orderByElement = new OrderByElement();
                    orderByElement.setExpression(column);
                    orderByElement.setAsc(j.getSortType() == SortType.ASC);
                    return orderByElement;
                })
                .collect(Collectors.toList());
        return PlainSelect.orderByToString(orderByElements).trim();
    }

    public static String getGroupByItems(EntityMeta meta){
        if(CollectionUtils.isEmpty(meta.getGroupBys())) return "";
        List<Expression> orderBys = new ArrayList<>();
        meta.getGroupBys().stream().forEach(fl -> {
            EntityField field = meta.resolve(fl);
            Column column = new Column(field.getTableAlias() + "." + field.getColumn());
            orderBys.add(column);
        });
        ExpressionList expressionList = new ExpressionList();
        expressionList.setExpressions(orderBys);
        GroupByElement groupByElement = new GroupByElement();
        groupByElement.setGroupByExpressionList(expressionList);
        return groupByElement.toString();
    }

    public static boolean isInsertExclude(EntityField f, EntityMeta meta){
        return !f.getTableAlias().equals(meta.getTableAlias())
                || f==meta.annoField(SoftDel.class);
    }

    public static String getInsertItems(EntityMeta meta){
        List<String> items = new ArrayList<>();
        meta.fields().stream().forEach(f -> {
            if(isInsertExclude(f, meta)) return;
            items.add(f.getColumn());
        });
        if(meta.isSoftDel()){
            items.add(meta.annoField(SoftDel.class).getColumn());
        }
        return items.stream().collect(Collectors.joining(","));
    }

    public static String getInsertValues(EntityMeta meta){
        List<String> values = new ArrayList<>();
        meta.fields().stream().forEach(f -> {
            if(isInsertExclude(f, meta)) return;
            values.add(String.format("#{%s}", f.getName()));
        });
        if(meta.isSoftDel()){
            values.add(String.format("#{%s}", meta.annoField(SoftDel.class).getName()));
        }
        return values.stream().collect(Collectors.joining(","));
    }

    public static boolean isUpdateExclude(EntityField f, EntityMeta meta){
        return !f.getTableAlias().equals(meta.getTableAlias())
                || f == meta.annoField(Id.class)
                || f == meta.annoField(SoftDel.class);
    }

    public static String getUpdateItems(EntityMeta meta){
        List<String> items = new ArrayList<>();
        meta.fields().stream().forEach(f -> {
            if(isUpdateExclude(f, meta)) return;
            items.add(String.format(
                    "<if test=\"%s != null\">%s = #{%s},</if>",
                    f.getName(),
                    f.getColumn(),
                    f.getName()));
        });
        return items.stream().collect(Collectors.joining(" "));
    }

}
