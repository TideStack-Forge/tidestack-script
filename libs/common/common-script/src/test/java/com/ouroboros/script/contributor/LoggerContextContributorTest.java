package com.ouroboros.script.contributor;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

public class LoggerContextContributorTest {

  @Test
  public void getBindings_exposesLoggerAliases() {
    var contributor = new LoggerContextContributor();

    var values = contributor.getBindings();

    assertTrue(values.containsKey("logger"));
    assertTrue(values.containsKey("console"));
    assertSame(values.get("logger"), values.get("console"));
    assertTrue(values.get("logger") instanceof LoggerContextContributor.LoggerAdapter);
  }

  @Test
  public void loggerAdapter_overloadsDoNotThrow() {
    var adapter = new LoggerContextContributor.LoggerAdapter();

    adapter.log("hello");
    adapter.log("hello {}", "world");
    adapter.log("hello {} {}", "world", 1);
    adapter.log("hello {} {} {}", "world", 1, true);
    adapter.log("hello {}", new Object[] {"world"});
    adapter.log("hello", new RuntimeException("boom"));

    adapter.trace("trace");
    adapter.trace("trace {}", "world");
    adapter.trace("trace {} {}", "world", 1);
    adapter.trace("trace {} {} {}", "world", 1, true);
    adapter.trace("trace", new RuntimeException("boom"));

    adapter.debug("debug");
    adapter.debug("debug {}", "world");
    adapter.debug("debug {} {}", "world", 1);
    adapter.debug("debug {} {} {}", "world", 1, true);
    adapter.debug("debug", new RuntimeException("boom"));

    adapter.info("info");
    adapter.info("info {}", "world");
    adapter.info("info {} {}", "world", 1);
    adapter.info("info {} {} {}", "world", 1, true);
    adapter.info("info", new RuntimeException("boom"));

    adapter.warn("warn");
    adapter.warn("warn {}", "world");
    adapter.warn("warn {} {} {}", "world", 1, true);
    adapter.warn("warn {} {}", "world", 1);
    adapter.warn("warn", new RuntimeException("boom"));

    adapter.error("error");
    adapter.error("error {}", "world");
    adapter.error("error {} {}", "world", 1);
    adapter.error("error {} {} {}", "world", 1, true);
    adapter.error("error", new RuntimeException("boom"));
  }
}
