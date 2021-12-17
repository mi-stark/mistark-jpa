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

    @TenantId
    @Column
    @JsonIgnore
    private Long tenantId;

    @CreateBy
    @Column
    @JsonString
    private Long createBy;

    @CreateDate
    @Column
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createDate;

    @UpdateBy
    @Column
    @JsonString
    private Long updateBy;

    @UpdateDate
    @Column
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateDate;

    @Version
    @Column
    @JsonString
    private Integer version;

    @SoftDel
    @Column
    @JsonIgnore
    private Integer deleted;

}
