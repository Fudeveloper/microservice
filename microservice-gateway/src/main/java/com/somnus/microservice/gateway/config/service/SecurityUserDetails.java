package com.somnus.microservice.gateway.config.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.io.Serializable;
import java.util.Collection;
/**
 * @author kevin.liu
 * @title: SecurityUserDetails
 * @projectName gateway
 * @description: TODO
 * @date 2021/11/15 15:19
 */
public class SecurityUserDetails extends User implements Serializable {

    @Getter
    @Setter
    private Long userId;

    public SecurityUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, Long userId) {
        super(username, password, authorities);
        this.userId = userId;
    }

    public SecurityUserDetails(String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities, Long userId) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.userId = userId;
    }
}