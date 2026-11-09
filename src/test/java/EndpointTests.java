import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import testframework.api.AppClient;
import testframework.helpers.TokenGenerator;
import testframework.models.EndpointResponse;
import testframework.wiremock.WireMockManager;
import testframework.wiremock.stubs.ActionStubs;
import testframework.wiremock.stubs.AuthStubs;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Epic("Тестирование эндпоинта /endpoint")
@Feature("Работа с токенами и действиями")
@DisplayName("/endpoint")
public class EndpointTests {

    private static final AppClient appClient = new AppClient();

    private static final String TOKEN_NOT_FOUND = "Token '%s' not found";
    private static final String TOKEN_ALREADY_EXISTS = "Token '%s' already exists";

    @BeforeAll
    @DisplayName("Настройка тестового окружения")
    @Step("Инициализация клиента и WireMock")
    static void setUpAll() {
        WireMockManager.start();
    }

    @BeforeEach
    @DisplayName("Сброс состояния перед тестом")
    @Step("Возвращение WireMock в исходное состояние")
    void setUp() {
        WireMockManager.reset();
    }

    @AfterAll
    @DisplayName("Очистка тестового окружения")
    @Step("Остановка WireMock")
    static void tearDown() {
        WireMockManager.stop();
    }

    @Test
    @DisplayName("Positive: Успешный логин пользователя")
    @Description("Пользователь с валидным токеном может успешно залогиниться")
    @Severity(SeverityLevel.BLOCKER)
    void successfulLoginTest() {
        String token = generateAndConfigureValidToken(200);

        Allure.step("Пользователь выполняет вход в систему", () -> {
            var response = performLoginRequest(token);

            Allure.step("Система подтверждает успешную аутентификацию", () -> {
                verifySuccessfulResponse(response);
            });
        });
    }

    @Test
    @DisplayName("Positive: LOGIN → ACTION")
    @Description("После успешного логина пользователь может выполнить действие")
    @Severity(SeverityLevel.CRITICAL)
    void doActionAfterSuccessfulLoginTest() {
        String token = generateAndConfigureValidToken(200);
        setupAction(token, 200);

        Allure.step("Пользователь авторизуется в системе", () -> {
            var loginResponse = performLoginRequest(token);
            verifySuccessfulResponse(loginResponse);
        });

        Allure.step("Пользователь выполняет действие", () -> {
            var actionResponse = performActionRequest(token);
            verifySuccessfulResponse(actionResponse);
        });
    }

    @Test
    @DisplayName("Positive: LOGIN → LOGOUT")
    @Description("После успешного логина пользователь может выполнить логаут без предворительного действия")
    @Severity(SeverityLevel.CRITICAL)
    void doLogoutAfterSuccessfulLoginTest() {
        String token = generateAndConfigureValidToken(200);
        setupAction(token, 200);

        Allure.step("Пользователь авторизуется в системе", () -> {
            var loginResponse = performLoginRequest(token);
            verifySuccessfulResponse(loginResponse);
        });

        Allure.step("Пользователь выходит из системы", () -> {
            var logoutResponse = performLogoutRequest(token);
            verifySuccessfulResponse(logoutResponse);
        });
    }

    @Test
    @DisplayName("Positive: Полный цикл: LOGIN → ACTION → LOGOUT")
    @Description("Пользователь может пройти полный цикл работы с приложением")
    @Severity(SeverityLevel.CRITICAL)
    void fullUserFlowTest() {
        String token = generateAndConfigureValidToken(200);
        setupAction(token, 200);

        Allure.step("Пользователь проходит полный сценарий работы с системой", () -> {

            Allure.step("Вход пользователя в систему", () -> {
                var loginResponse = performLoginRequest(token);
                verifySuccessfulResponse(loginResponse);
            });

            Allure.step("Выполнение пользовательского действия", () -> {
                var actionResponse = performActionRequest(token);
                verifySuccessfulResponse(actionResponse);
            });

            Allure.step("Выход пользователя из системы", () -> {
                var logoutResponse = performLogoutRequest(token);
                verifySuccessfulResponse(logoutResponse);
            });
        });
    }

    @Test
    @DisplayName("Positive: Повторный LOGIN с тем же токеном после LOGOUT")
    @Description("LOGOUT удаляет токен из внутреннего хранилища")
    @Severity(SeverityLevel.NORMAL)
    void multipleLoginWithSameTokenAfterLogoutTest() {
        String token = generateAndConfigureValidToken(200);

        Allure.step("Пользователь входит в систему", () -> {
            verifySuccessfulResponse(performLoginRequest(token));
        });

        Allure.step("Пользователь выходит из системы", () -> {
            verifySuccessfulResponse(performLogoutRequest(token));
        });

        Allure.step("Пользователь повторно входит с тем же токеном", () -> {
            var secondLogin = performLoginRequest(token);
            verifySuccessfulResponse(secondLogin);
        });
    }

    @Test
    @DisplayName("Positive: Два пользователя работают параллельно")
    @Description("Несколько пользователей могут работать параллельно")
    @Severity(SeverityLevel.NORMAL)
    void twoUsersSimultaneouslyTest() {
        String token1 = generateAndConfigureValidToken(200);
        setupAction(token1, 200);

        String token2 = generateAndConfigureValidToken(200);
        setupAction(token2, 200);

        Allure.step("Первый пользователь проходит сценарий LOGIN → ACTION", () -> {
            verifySuccessfulResponse(performLoginRequest(token1));
            verifySuccessfulResponse(performActionRequest(token1));
        });

        Allure.step("Второй пользователь независимо проходит сценарий LOGIN → ACTION", () -> {
            verifySuccessfulResponse(performLoginRequest(token2));
            verifySuccessfulResponse(performActionRequest(token2));
        });
    }

    @Test
    @DisplayName("Negative: ACTION без предварительного LOGIN")
    @Description("Попытка выполнить ACTION без LOGIN")
    @Severity(SeverityLevel.CRITICAL)
    void doActionWithoutLoginTest() {
        String token = TokenGenerator.generateValidToken();
        setupAction(token, 200);

        Allure.step("Пользователь пытается выполнить действие без авторизации", () -> {
            var response = performActionRequest(token);

            Allure.step("Система отклоняет запрос", () -> {
                verifyErrorResponse(response, String.format(TOKEN_NOT_FOUND, token));
            });
        });
    }

    @Test
    @DisplayName("Negative: Повторный LOGIN с тем же токеном")
    @Description("Пользователь не может залогиниться с токеном, который уже есть в хранилище")
    @Severity(SeverityLevel.NORMAL)
    void multipleLoginWithSameTokenTest() {
        String token = generateAndConfigureValidToken(200);

        Allure.step("Пользователь успешно входит в систему", () -> {
            verifySuccessfulResponse(performLoginRequest(token));
        });

        Allure.step("Пользователь пытается войти повторно с тем же токеном", () -> {
            var secondLogin = performLoginRequest(token);
            verifyErrorResponse(secondLogin, String.format(TOKEN_ALREADY_EXISTS, token));
        });
    }

    @Test
    @DisplayName("Negative: ACTION после неуспешного LOGIN")
    @Description("Попытка выполнить ACTION без успешного LOGIN")
    @Severity(SeverityLevel.CRITICAL)
    void doActionWithFailureLoginTest() {
        String token = generateAndConfigureValidToken(401);
        setupAction(token, 200);

        Allure.step("Авторизация пользователя завершается ошибкой", () -> {
            performLoginRequest(token);
        });

        Allure.step("Пользователь пытается выполнить действие", () -> {
            var response = performActionRequest(token);
            verifyErrorResponse(response, String.format(TOKEN_NOT_FOUND, token));
        });
    }

    @Test
    @DisplayName("Negative: LOGOUT без предварительного LOGIN")
    @Description("Попытка выйти из системы без входа")
    @Severity(SeverityLevel.NORMAL)
    void logoutWithoutLoginTest() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Пользователь пытается выйти без входа в систему", () -> {
            var response = performLogoutRequest(token);
            verifyErrorResponse(response, String.format(TOKEN_NOT_FOUND, token));
        });
    }

    @Test
    @DisplayName("Negative: ACTION после LOGOUT должен завершиться ошибкой")
    @Description("Попытка выполнить действие после LOGOUT")
    @Severity(SeverityLevel.CRITICAL)
    void actionAfterLogoutShouldFailTest() {
        String token = generateAndConfigureValidToken(200);
        setupAction(token, 200);

        Allure.step("Пользователь проходит LOGIN → LOGOUT", () -> {
            verifySuccessfulResponse(performLoginRequest(token));
            verifySuccessfulResponse(performLogoutRequest(token));
        });

        Allure.step("Пользователь пытается выполнить действие после выхода", () -> {
            var response = performActionRequest(token);
            verifyErrorResponse(response, String.format(TOKEN_NOT_FOUND, token));
        });
    }

    @Test
    @DisplayName("Negative: Повторный LOGOUT одного токена")
    @Description("Попытка выполнить LOGOUT повторно")
    @Severity(SeverityLevel.NORMAL)
    void secondLogoutShouldFailTest() {
        String token = generateAndConfigureValidToken(200);

        Allure.step("Пользователь выходит из системы", () -> {
            verifySuccessfulResponse(performLoginRequest(token));
            verifySuccessfulResponse(performLogoutRequest(token));
        });

        Allure.step("Пользователь пытается выйти повторно", () -> {
            var secondLogout = performLogoutRequest(token);
            verifyErrorResponse(secondLogout, String.format(TOKEN_NOT_FOUND, token));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "123",
            "abc",
            "ABC123",
            "ABC123DEF456ABC789ABC012ABC345ABC123",
            "abc123def456def789def012def345def",
            "ABC123!@#DEF456GHI789JKL012MNO345",
            "АБЦ123DEF456ABC789ABC012ABC345ABC"
    })
    @DisplayName("LOGIN с невалидным форматом токена")
    @Description("Система валидирует формат токена перед отправкой во внешний сервис")
    @Severity(SeverityLevel.NORMAL)
    void loginWithInvalidTokenFormatTest(String invalidToken) {
        Allure.parameter("Токен", invalidToken);
        Allure.parameter("Длина", String.valueOf(invalidToken.length()));

        var response = performLoginRequest(invalidToken);

        verifyErrorResponse(response, "token: должно соответствовать \"^[0-9A-F]{32}$\"");
    }

    @ParameterizedTest
    @MethodSource("provideExternalServiceErrorCodes")
    @DisplayName("LOGIN при различных ошибках внешнего сервиса")
    @Description("Система обрабатывает разные HTTP статусы от внешнего сервиса")
    @Severity(SeverityLevel.NORMAL)
    void loginWithDifferentExternalServiceErrorsTest(Integer statusCode, String expectedError) {
        Allure.parameter("HTTP статус", String.valueOf(statusCode));
        String token = generateAndConfigureValidToken(statusCode);

        var response = performLoginRequest(token);

        verifyErrorResponse(response, expectedError);
    }

    @ParameterizedTest
    @MethodSource("provideExternalServiceErrorCodes")
    @DisplayName("ACTION при различных ошибках внешнего сервиса")
    @Description("Система обрабатывает разные HTTP статусы от внешнего сервиса")
    @Severity(SeverityLevel.NORMAL)
    void actionWithDifferentExternalServiceErrorsTest(Integer statusCode, String expectedError) {
        Allure.parameter("HTTP статус", String.valueOf(statusCode));
        String token = generateAndConfigureValidToken(200);
        setupAction(token, statusCode);

        performLoginRequest(token);
        var response = performActionRequest(token);

        verifyErrorResponse(response, expectedError);
    }

    private static Stream<Arguments> provideExternalServiceErrorCodes() {
        return Stream.of(
                arguments(400, "Internal Server Error"),
                arguments(403, "Internal Server Error"),
                arguments(404, "Internal Server Error"),
                arguments(408, "Internal Server Error"),
                arguments(429, "Internal Server Error"),
                arguments(500, "Internal Server Error"),
                arguments(503, "Internal Server Error")
        );
    }

    @Step("Генерация токена и настройка /auth внешнего сервиса на статус {statusCode}")
    private String generateAndConfigureValidToken(Integer statusCode) {
        String token = TokenGenerator.generateValidToken();
        AuthStubs.setupAuthWithStatus(token, statusCode);
        Allure.addAttachment("Сгенерированный токен", "text/plain", token);
        Allure.addAttachment("Статус /auth внешнего сервиса", "text/plain", String.valueOf(statusCode));
        return token;
    }

    @Step("Настройка /doAction внешнего сервиса на статус {statusCode}")
    private void setupAction(String token, Integer statusCode) {
        ActionStubs.setupActionWithStatus(token, statusCode);
        Allure.addAttachment("Настроен ACTION", "text/plain", "Для токена: " + token);
        Allure.addAttachment("Статус /doAction внешнего сервиса", "text/plain", String.valueOf(statusCode));
    }

    @Step("Пользователь выполняет вход в систему")
    protected EndpointResponse performLoginRequest(String token) {
        return appClient.login(token);
    }

    @Step("Пользователь выполняет действие")
    protected EndpointResponse performActionRequest(String token) {
        return appClient.action(token);
    }

    @Step("Пользователь выходит из системы")
    protected EndpointResponse performLogoutRequest(String token) {
        return appClient.logout(token);
    }

    @Step("Проверка успешного ответа")
    private void verifySuccessfulResponse(EndpointResponse response) {
        Allure.addAttachment("Ответ системы", response.toString());
        assertEquals("OK", response.result());
        assertNull(response.message(), "При успехе поле 'message' должно быть null");
    }

    @Step("Проверка ошибочного ответа")
    private void verifyErrorResponse(EndpointResponse response, String expectedErrorContext) {
        Allure.addAttachment("Ответ системы", response.toString());
        assertEquals("ERROR", response.result());
        assertEquals(expectedErrorContext, response.message());
    }
}
