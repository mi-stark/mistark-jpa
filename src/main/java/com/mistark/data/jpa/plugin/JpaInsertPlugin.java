package com.mistark.data.jpa.plugin;

import com.mistark.data.jpa.annotation.*;
import com.mistark.data.jpa.helper.EntityHelper;
import com.mistark.data.jpa.helper.SoftDelHelper;
import com.mistark.data.jpa.meta.EntityMeta;
import com.mistark.data.jpa.meta.EntityMeta.*;
import com.mistark.data.jpa.support.IdGenerator;
import com.mistark.meta.time.Clock;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(PluginOrders.INSERT)
public class JpaInsertPlugin implements JpaPlugin {

    @Resource
    IdGenerator idGenerator;
    
    @Resource
    PluginConfig pluginConfig;

    @Override
    public boolean match(MappedStatement ms, BoundSql boundSql) {
        return ms.getSqlCommandType() == SqlCommandType.INSERT;
    }

    @Override
    public void patch(MappedStatement ms, BoundSql boundSql) throws Throwable {
        Object pi = boundSql.getParameterObject();
        EntityMeta meta = EntityHelper.fromStatement(ms);
        if(meta == null || (!meta.getEntity().equals(pi.getClass()) && !(pi instanceof Map))) return;
        MetaObject metaObject = SystemMetaObject.forObject(pi);
        Map<EntityField, Object> metaValue = new HashMap<>();

        if(meta.hasAnnoField(Id.class)){
            Object id = metaObject.getValue(meta.annoFieldName(Id.class));
            if(id == null){
                id = Number.class.isAssignableFrom(meta.annoFieldType(Id.class))
                        ? idGenerator.nextId()
                        : idGenerator.nextUUID();
                metaValue.put(meta.annoField(Id.class), id);
            }
        }

        Date now = Clock.currentDate();
        if(meta.hasAnnoField(CreateDate.class)){
            metaValue.put(meta.annoField(CreateDate.class), now);
        }
        
        if(meta.hasAnnoField(UpdateDate.class)){
            metaValue.put(meta.annoField(UpdateDate.class), now);
        }
        
        if(meta.hasAnnoField(Version.class)){
            metaValue.put(meta.annoField(Version.class), 0);
        }

        if(meta.isSoftDel()){
            metaValue.put(meta.annoField(SoftDel.class), SoftDelHelper.getValue(false, meta));
        }

        if(pluginConfig.hasUser()){
            Object userId = pluginConfig.getUserId();
            if(meta.hasAnnoField(CreateBy.class)){
                metaValue.put(meta.annoField(CreateBy.class), userId);
            }
            if(meta.hasAnnoField(UpdateBy.class)){
                metaValue.put(meta.annoField(UpdateBy.class), userId);
            }
        }
        
        if(pluginConfig.hasTenant()){
            Object tenantId = pluginConfig.getTenantId();
            if(meta.hasAnnoField(TenantId.class)){
                metaValue.put(meta.annoField(TenantId.class), tenantId);
            }
        }

        metaValue.entrySet().forEach( entry -> {
            Object target = ConvertUtils.convert(entry.getValue(), entry.getKey().getJavaType());
            metaObject.setValue(entry.getKey().getName(), target);
        });
    }
}
