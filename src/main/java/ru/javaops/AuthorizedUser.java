package ru.javaops;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.javaops.model.User;
import ru.javaops.to.AuthUser;

/**
 * GKislin
 */
@Slf4j
public class AuthorizedUser extends org.springframework.security.core.userdetails.User {
    private static final long serialVersionUID = 1L;

    private AuthUser user;

    public AuthorizedUser(User user) {
        super(user.getEmail(), user.getPassword() != null ? user.getPassword() : "dummy", true, true, true, true, user.getRoles());
        this.user = new AuthUser(user);
    }

    public static AuthUser user() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        Object user = auth.getPrincipal();
        return (user instanceof AuthUser) ? (AuthUser) user : null;
    }

    public static boolean isAuthorized() {
        return user() != null;
    }

    public static AuthUser authUser() {
        AuthUser user = user();
        if (user == null) {
            throw new AccessDeniedException("Требуется авторизация");
        }
        return user;
    }

    @Override
    public String toString() {
        return user == null ? "noAuth" : user.toString();
    }
}
