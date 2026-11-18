package dev.sre.auction.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeploymentInfoControllerTest {

    @Test
    void exposesConfiguredVersionAndInstance() {
        DeploymentInfoController controller = new DeploymentInfoController(
                "auction-api",
                "v2.0.0",
                "auction-api-v2-abc"
        );

        DeploymentInfoController.DeploymentInfoResponse response = controller.get();

        assertThat(response.service()).isEqualTo("auction-api");
        assertThat(response.version()).isEqualTo("v2.0.0");
        assertThat(response.instance()).isEqualTo("auction-api-v2-abc");
    }
}
