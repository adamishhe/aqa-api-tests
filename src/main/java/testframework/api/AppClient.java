package testframework.api;

import io.restassured.http.ContentType;
import testframework.enums.Action;
import testframework.models.EndpointResponse;

import static io.restassured.RestAssured.given;
import static testframework.enums.Action.*;

public class AppClient {
    private static final String BASE_URL = "http://localhost:8080";
    private static final String API_KEY = "qazWSXedc";

    public EndpointResponse login(String token) {
        return send(LOGIN, token);
    }

    public EndpointResponse action(String token) {
        return send(ACTION, token);
    }

    public EndpointResponse logout(String token) {
        return send(LOGOUT, token);
    }

    private EndpointResponse send(Action action, String token) {
        return given()
                .baseUri(BASE_URL)
                .contentType(ContentType.URLENC)
                .header("X-Api-Key", API_KEY)
                .formParam("token", token)
                .formParam("action", action)
                .post("/endpoint")
                .as(EndpointResponse.class);
    }
}
