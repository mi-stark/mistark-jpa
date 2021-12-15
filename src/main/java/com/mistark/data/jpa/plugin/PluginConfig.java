package com.mistark.data.jpa.plugin;

import com.mistark.data.jpa.support.TenantIdService;
import com.mistark.data.jpa.support.UserIdService;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PluginConfig {
    private TenantIdService tenantIdService;
    private UserIdService userIdService;

}
