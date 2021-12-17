package com.mistark.data.jpa.plugin;

import com.mistark.data.jpa.helper.EntityHelper;
import com.mistark.data.jpa.helper.SoftDelHelper;
import com.mistark.data.jpa.meta.EntityField;
import com.mistark.data.jpa.meta.EntityMeta;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(PluginOrder.INSERT)
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
        Map<EntityField, Object> metaValue = new ConcurrentHashMap<>();

        if(meta.getId()!=null){
            Object id = metaObject.getValue(meta.getId().getName());
            if(id == null){
                id = Number.class.isAssignableFrom(meta.getId().getJavaType())
                        ? idGenerator.nextId()
                        : idGenerator.nextUUID();
                metaValue.put(meta.getId(), id);
            }
        }

        Date now = Clock.currentDate();
        if(meta.getCreateDate()!=null){
            metaValue.put(meta.getCreateDate(), now);
        }
        
        if(meta.getUpdateDate()!=null){
            metaValue.put(meta.getUpdateDate(), now);
        }
        
        if(meta.getVersion()!=null){
            metaValue.put(meta.getVersion(), 0);
        }

        if(meta.isSoftDel()){
            metaValue.put(meta.getSoftDel(), SoftDelHelper.getValue(false, meta));
        }

        if(pluginConfig.getUserIdService()!=null){
            Object userId = pluginConfig.getUserIdService().getUserId();
            if(meta.getCreateBy()!=null){
                metaValue.put(meta.getCreateBy(), userId);
            }
            if(meta.getUpdateBy()!=null){
                metaValue.put(meta.getUpdateBy(), userId);
            }
        }
        
        if(pluginConfig.getTenantIdService()!=null){
            Object tenantId = pluginConfig.getTenantIdService().getTenantId();
            if(meta.getTenantId()!=null){
                metaValue.put(meta.getTenantId(), tenantId);
            }
        }

        metaValue.entrySet().forEach( entry -> {
            Object target = ConvertUtils.convert(entry.getValue(), entry.getKey().getJavaType());
            metaObject.setValue(entry.getKey().getName(), target);
        });
    }
}
