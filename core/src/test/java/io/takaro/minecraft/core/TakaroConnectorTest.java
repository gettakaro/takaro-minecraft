package io.takaro.minecraft.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TakaroConnectorTest {

    private static class TestAdapter implements GameAdapter {
        final List<String> infos = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        final List<String> debugs = new ArrayList<>();

        @Override
        public void logInfo(String msg) { infos.add(msg); }

        @Override
        public void logWarning(String msg) { warnings.add(msg); }

        @Override
        public void logDebug(String msg) { debugs.add(msg); }

        @Override
        public void runOnMainThread(Runnable task) { task.run(); }
    }

    @Test
    void connectWithNullUrlLogsWarning() {
        TestAdapter adapter = new TestAdapter();
        TakaroConfig config = new TakaroConfig();

        TakaroConnector connector = new TakaroConnector(adapter, config);
        connector.connect();

        assertEquals(1, adapter.warnings.size());
        assertTrue(adapter.warnings.get(0).contains("No WebSocket URL configured"));
        assertTrue(adapter.infos.isEmpty());
    }

    @Test
    void connectWithEmptyUrlLogsWarning() {
        TestAdapter adapter = new TestAdapter();
        TakaroConfig config = new TakaroConfig();
        config.setWsUrl("");

        TakaroConnector connector = new TakaroConnector(adapter, config);
        connector.connect();

        assertEquals(1, adapter.warnings.size());
        assertTrue(adapter.warnings.get(0).contains("No WebSocket URL configured"));
    }

    @Test
    void connectWithInvalidUrlLogsError() {
        TestAdapter adapter = new TestAdapter();
        TakaroConfig config = new TakaroConfig();
        config.setWsUrl("not a valid url %%");

        TakaroConnector connector = new TakaroConnector(adapter, config);
        connector.connect();

        assertEquals(1, adapter.infos.size());
        assertTrue(adapter.infos.get(0).contains("Connecting to Takaro"));
        assertEquals(1, adapter.warnings.size());
        assertTrue(adapter.warnings.get(0).contains("Failed to create WebSocket connection"));
    }

    @Test
    void shutdownWithNoConnectionDoesNotThrow() {
        TestAdapter adapter = new TestAdapter();
        TakaroConfig config = new TakaroConfig();

        TakaroConnector connector = new TakaroConnector(adapter, config);
        assertDoesNotThrow(connector::shutdown);
    }
}
