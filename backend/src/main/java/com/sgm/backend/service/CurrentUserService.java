package com.sgm.backend.service;

import com.sgm.backend.exception.ApiException;
import com.sgm.backend.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    public UserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw ApiException.forbidden("No hay una sesion activa");
        }
        return principal;
    }

    public Long userId() {
        return principal().userId();
    }

    public String machineId() {
        return principal().machineId();
    }
}