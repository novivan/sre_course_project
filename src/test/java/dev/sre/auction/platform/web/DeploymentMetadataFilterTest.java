package dev.sre.auction.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DeploymentMetadataFilterTest {

    @Test
    void addsDeploymentMetadataToEveryResponse() throws Exception {
        DeploymentMetadataFilter filter = new DeploymentMetadataFilter("v1.0.0", "api-pod-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                new MockHttpServletRequest("GET", "/api/v1/lots"),
                response,
                (request, servletResponse) -> { }
        );

        assertThat(response.getHeader("X-App-Version")).isEqualTo("v1.0.0");
        assertThat(response.getHeader("X-Pod-Name")).isEqualTo("api-pod-1");
    }
}
