package testframework.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@UtilityClass
public class WireMockManager {
    public static final Logger log = LoggerFactory.getLogger(WireMockManager.class);
    private static final int PORT = 8888;

    private WireMockServer wireMockServer;

    public void start() {
        if (isRunning()) {
            log.info("WireMock уже запущен на порту {}", PORT);
            return;
        }

        wireMockServer = new WireMockServer(options().port(PORT));
        wireMockServer.start();
        WireMock.configureFor("localhost", PORT);

        log.info("WireMock запущен на http://localhost:{}", PORT);
    }

    public void stop() {
        if (isRunning()) {
            wireMockServer.stop();
            log.info("WireMock остановлен");
        }
    }

    public void reset() {
        if (isRunning()) {
            WireMock.reset();
            log.debug("WireMock сброшен");
        }
    }

    public boolean isRunning() {
        return wireMockServer != null && wireMockServer.isRunning();
    }
}
