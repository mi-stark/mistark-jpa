package com.mistark.data.jpa.support;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mistark.data.jpa.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class BaseEntity {

    @Id
    @Column(name = "rowid")
    @JsonString
    private Long id;

    @Column
    @TenantId
    @JsonIgnore
    private Long tenantId;

    @Column
    @CreateBy
    private Long createBy;

    @Column
    @CreateDate
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createDate;

    @Column
    @UpdateBy
    private Long updateBy;

    @Column
    @UpdateDate
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateDate;

    @Column
    @SoftDel
    @JsonIgnore
    private Integer deleted;

}
