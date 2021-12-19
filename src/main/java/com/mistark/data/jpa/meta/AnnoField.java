package com.mistark.data.jpa.meta;

import com.mistark.data.jpa.helper.AnnoFieldHelper;
import lombok.Setter;
import org.apache.ibatis.builder.BuilderException;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;

public class AnnoField {

    private Annotation anno;
    private Class<? extends Annotation> annoType;
    private String fieldKey;
    private String enableKey;
    private Class[] types;
    @Setter
    private EntityMeta.EntityField entityField;

    public AnnoField(Annotation anno, Class annoType){
        this.anno = anno;
        this.annoType = annoType;
        this.fieldKey = AnnoFieldHelper.resolveFieldKey(annoType);
        this.enableKey = AnnoFieldHelper.resolveEnableKey(annoType);
        this.types = AnnoFieldHelper.resolveTypes(annoType);
    }

    public String getField(){
        Object field = AnnotationUtils.getValue(anno, fieldKey);
        if(field == null) return "";
        return field.toString();
    }

    public boolean isEnabled(){
        Object enable = AnnotationUtils.getValue(anno, enableKey);
        if(enable == null) return true;
        return (boolean) enable;
    }

    public void checkType(){
        if(types == null || types.length==0) return;
        Class targetType = entityField.getJavaType();
        if(!Arrays.stream(types).anyMatch(type -> type.isAssignableFrom(targetType))){
            throw new BuilderException(String.format("wrong type for @%s：%s", annoType.getSimpleName(), targetType.getName()));
        }
    }
}
