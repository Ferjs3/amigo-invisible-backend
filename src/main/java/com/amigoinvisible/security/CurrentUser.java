package com.amigoinvisible.security;

import com.amigoinvisible.entity.User;
import com.amigoinvisible.exception.ForbiddenException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public User get() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        if (!(principal instanceof User user)) {
            throw new ForbiddenException("No autenticado");
        }
        return user;
    }
}
