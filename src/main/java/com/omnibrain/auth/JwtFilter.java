package com.omnibrain.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Authenticates requests that carry a valid {@code Authorization: Bearer <jwt>} header.
 * Registered explicitly in {@code SecurityConfig} (not a @Component, so it runs once, inside
 * the security filter chain).
 */
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final Set<String> adminUsers;

    public JwtFilter(JwtUtil jwtUtil, Set<String> adminUsers) {
        this.jwtUtil = jwtUtil;
        this.adminUsers = adminUsers;
    }

    /**
     * Streaming (SSE) responses finish with an ASYNC dispatch that passes through the security chain
     * again. By default OncePerRequestFilter skips it, which would leave that pass unauthenticated and
     * make Spring Security reject the end of every stream.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            jwtUtil.parseUsername(header.substring(7)).ifPresent(username -> {
                List<GrantedAuthority> authorities = adminUsers.contains(username)
                        ? List.of(new SimpleGrantedAuthority("ROLE_USER"),
                                  new SimpleGrantedAuthority("ROLE_ADMIN"))
                        : List.of(new SimpleGrantedAuthority("ROLE_USER"));
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        filterChain.doFilter(request, response);
    }
}
