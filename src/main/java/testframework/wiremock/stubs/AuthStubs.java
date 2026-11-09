package testframework.wiremock.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class AuthStubs {

    public static void setupAuthWithStatus(String token, int statusCode) {
        stubFor(post(urlEqualTo("/auth"))
                .withRequestBody(containing("token=" + token))
                .willReturn(aResponse()
                        .withStatus(statusCode)));
    }

}
