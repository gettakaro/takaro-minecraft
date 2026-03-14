# Testing

## Framework

JUnit Jupiter 5.11.4 with JUnit Platform Launcher. Tests exist only in the `core/` module — platform modules have no tests.

## Commands

### Run All Tests

```bash
./gradlew :core:test
```

25 tests, runs in ~2 seconds.

### Run Specific Test Class

```bash
./gradlew :core:test --tests "io.takaro.minecraft.core.TakaroConfigTest"
```

### Run Specific Test Method

```bash
./gradlew :core:test --tests "io.takaro.minecraft.core.TakaroConfigTest.defaultValuesAreSet"
```

## Test Files

All in `core/src/test/java/io/takaro/minecraft/core/`:

| File | What it tests |
|------|---------------|
| `TakaroConfigTest.java` | Config defaults, getters/setters, env var overrides |
| `TakaroConnectorTest.java` | WebSocket connector initialization |
| `ActionRoutingTest.java` | Action message routing and dispatch |

## Test Reports

HTML report: `core/build/reports/tests/test/index.html`

## Notes

- No setup required (no Docker services, no env vars needed)
- Platform modules (paper, neoforge, fabric) have no tests — testing happens via Docker dev servers and the test bot
