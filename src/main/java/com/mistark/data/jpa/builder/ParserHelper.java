package com.mistark.data.jpa.builder;

import com.mistark.data.jpa.helper.EntityHelper;
import com.mistark.data.jpa.meta.EntityField;
import com.mistark.data.jpa.meta.EntityMeta;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ParserHelper {

    public static String getSelectItems(EntityMeta meta){
        return meta.fields()
                .stream()
                .map(f-> String.format("%s.%s AS %s",
                        f.getTable(),
                        f.getColumn(),
                        f.getName()))
                .collect(Collectors.joining(","));
    }

    public static String getJoinItems(EntityMeta meta){
        if(CollectionUtils.isEmpty(meta.getJoins())) return "";
        return meta.getJoins()
                .stream()
                .map(j -> {
                    EntityMeta m = EntityHelper.resolve(j.getEntity());
                    return String.format(
                            "%s JOIN %s %s ON %s = %s",
                            j.getJoinType(),
                            m.getTable(),
                            j.getAlias(),
                            j.getOnLeft(),
                            j.getOnRight());
                })
                .collect(Collectors.joining(" "));
    }

    public static boolean isInsertExclude(EntityField f, EntityMeta meta){
        return !f.getTable().equals(EntityMeta.ALIAS)
                || f==meta.getSoftDel();
    }

    public static String getInsertItems(EntityMeta meta){
        List<String> items = new ArrayList<>();
        meta.fields().stream().forEach(f -> {
            if(isInsertExclude(f, meta)) return;
            items.add(f.getColumn());
        });
        if(meta.isSoftDel()){
            items.add(meta.getSoftDel().getColumn());
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
            values.add(String.format("#{%s}", meta.getSoftDel().getName()));
        }
        return values.stream().collect(Collectors.joining(","));
    }

    public static boolean isUpdateExclude(EntityField f, EntityMeta meta){
        return f.getTable().equals(EntityMeta.ALIAS)
                || f == meta.getId()
                || f == meta.getSoftDel();
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
        return items.stream().collect(Collectors.joining(","));
    }

}
