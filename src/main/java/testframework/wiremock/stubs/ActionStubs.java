package testframework.wiremock.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ActionStubs {
    public static void setupActionWithStatus(String token, int statusCode) {
        stubFor(post(urlEqualTo("/doAction"))
                .withRequestBody(containing("token=" + token))
                .willReturn(aResponse().withStatus(statusCode)));
    }
}
