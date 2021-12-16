package com.mistark.data.jpa.support;

import com.mistark.data.jpa.meta.Query;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class BaseService<T extends BaseEntity> {

    @Autowired
    private BaseMapper<T> baseMapper;

    public long queryTotal(Query query){
        query = new Query(query).total();
        return baseMapper.queryList(query).get(0).getId();
    }

    public T queryOne(Query query){
        query = new Query(query).limit(1);
        List<T> list = baseMapper.queryList(query);
        return list.size() > 0 ? list.get(0) : null;
    }

    public List<T> queryList(Query query){
        return baseMapper.queryList(query);
    }

    public T queryById(T entity){
        return baseMapper.queryById(entity);
    }

    public int insert(T entity){
        return baseMapper.updateById(entity);
    }

    public int save(T entity){
        if(entity.getId()!=null && queryById(entity)!=null){
            return updateById(entity);
        }else {
            return insert(entity);
        }
    }

    public int updateById(T entity){
        return baseMapper.updateById(entity);
    }

    public int deleteById(T entity){
        return baseMapper.deleteById(entity);
    }

}
