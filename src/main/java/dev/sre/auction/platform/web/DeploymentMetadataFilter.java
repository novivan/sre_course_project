package dev.sre.auction.platform.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnProperty(name = "app.role", havingValue = "api", matchIfMissing = true)
public class DeploymentMetadataFilter extends OncePerRequestFilter {

    private final String version;
    private final String instance;

    public DeploymentMetadataFilter(
            @Value("${app.version}") String version,
            @Value("${app.instance}") String instance
    ) {
        this.version = version;
        this.instance = instance;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        response.setHeader("X-App-Version", version);
        response.setHeader("X-Pod-Name", instance);
        filterChain.doFilter(request, response);
    }
}
