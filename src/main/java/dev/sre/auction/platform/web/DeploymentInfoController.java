package dev.sre.auction.platform.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/version")
@ConditionalOnProperty(name = "app.role", havingValue = "api", matchIfMissing = true)
public class DeploymentInfoController {

    private static final Logger log = LoggerFactory.getLogger(DeploymentInfoController.class);

    private final String service;
    private final String version;
    private final String instance;

    public DeploymentInfoController(
            @Value("${spring.application.name}") String service,
            @Value("${app.version}") String version,
            @Value("${app.instance}") String instance
    ) {
        this.service = service;
        this.version = version;
        this.instance = instance;
    }

    @GetMapping
    public DeploymentInfoResponse get() {
        log.info("Deployment info requested: version={}, instance={}", version, instance);
        return new DeploymentInfoResponse(service, version, instance);
    }

    public record DeploymentInfoResponse(String service, String version, String instance) {
    }
}
