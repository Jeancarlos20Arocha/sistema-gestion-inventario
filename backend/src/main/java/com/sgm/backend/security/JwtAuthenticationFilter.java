package com.sgm.backend.security;

import com.sgm.shared.constant.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null
                && SecurityContextHolder.getContext().getAuthentication() == null
                && jwtService.isValid(token, SecurityConstants.SCOPE_ACCESS)) {

            String username = jwtService.extractUsername(token);
            Long userId = asLong(jwtService.claim(token, SecurityConstants.CLAIM_USER_ID));
            String role = (String) jwtService.claim(token, SecurityConstants.CLAIM_ROLE);
            String machineId = (String) jwtService.claim(token, SecurityConstants.CLAIM_MACHINE_ID);

            if (username != null && userId != null) {
                UserPrincipal principal = new UserPrincipal(
                        userId, username, role, machineId, true, false);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.HEADER);
        if (StringUtils.hasText(header) && header.startsWith(SecurityConstants.PREFIX)) {
            return header.substring(SecurityConstants.PREFIX.length());
        }
        return null;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value != null ? Long.valueOf(value.toString()) : null;
    }
}