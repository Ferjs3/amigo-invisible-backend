package com.amigoinvisible.security;

import com.amigoinvisible.entity.AuthToken;
import com.amigoinvisible.entity.User;
import com.amigoinvisible.repository.AuthTokenRepository;
import com.amigoinvisible.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

// Autenticacion simple por token opaco (no JWT): el cliente manda
// "Authorization: Bearer <token>" y aca lo buscamos en la tabla auth_tokens.
// Si el token no existe o vencio, la request sigue como anonima y
// Spring Security se encarga de rechazarla en los endpoints protegidos.
@Component
@RequiredArgsConstructor
public class TokenAuthFilter extends OncePerRequestFilter {

    private final AuthTokenRepository authTokenRepository;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String rawToken = header.substring(7).trim();
            Optional<AuthToken> authToken = authTokenRepository.findByToken(rawToken);

            if (authToken.isPresent() && !isExpired(authToken.get())) {
                // Cargamos el usuario fresco y completo (no la referencia "perezosa"
                // que cuelga de AuthToken.user), asi evitamos un LazyInitializationException
                // mas tarde, cuando Hibernate ya cerro la sesion de esta request.
                Long userId = authToken.get().getUser().getId();
                userRepository.findById(userId).ifPresent(user -> {
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExpired(AuthToken token) {
        return token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now());
    }
}
