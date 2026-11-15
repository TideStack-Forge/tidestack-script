package com.ouroboros.script.contributor;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ouroboros.script.ScriptContextContributor;

public class LoggerContextContributor implements ScriptContextContributor {
  static final Logger logger = LoggerFactory.getLogger("ScriptEngine");
  static final LoggerAdapter loggerAdapter = new LoggerAdapter();

  @Override
  public Map<String, Object> getBindings() {
    var map = new HashMap<String, Object>();
    map.put("logger", loggerAdapter);
    map.put("console", loggerAdapter);
    return map;
  }

  public static class LoggerAdapter {
    public void log(String s) {
      logger.info(s);
    }

    public void log(String s, Object o) {
      logger.info(s, o);
    }

    public void log(String s, Object o, Object o1) {
      logger.info(s, o, o1);
    }

    public void log(String s, Object... objects) {
      logger.info(s, objects);
    }

    public void log(String s, Throwable throwable) {
      logger.info(s, throwable);
    }

    public void trace(String s) {
      logger.trace(s);
    }

    public void trace(String s, Object o) {
      logger.trace(s, o);
    }

    public void trace(String s, Object o, Object o1) {
      logger.trace(s, o, o1);
    }

    public void trace(String s, Object... objects) {
      logger.trace(s, objects);
    }

    public void trace(String s, Throwable throwable) {
      logger.trace(s, throwable);
    }

    public void debug(String s) {
      logger.debug(s);
    }

    public void debug(String s, Object o) {
      logger.debug(s, o);
    }

    public void debug(String s, Object o, Object o1) {
      logger.debug(s, o, o1);
    }

    public void debug(String s, Object... objects) {
      logger.debug(s, objects);
    }

    public void debug(String s, Throwable throwable) {
      logger.debug(s, throwable);
    }

    public void info(String s) {
      logger.info(s);
    }

    public void info(String s, Object o) {
      logger.info(s, o);
    }

    public void info(String s, Object o, Object o1) {
      logger.info(s, o, o1);
    }

    public void info(String s, Object... objects) {
      logger.info(s, objects);
    }

    public void info(String s, Throwable throwable) {
      logger.info(s, throwable);
    }

    public void warn(String s) {
      logger.warn(s);
    }

    public void warn(String s, Object o) {
      logger.warn(s, o);
    }

    public void warn(String s, Object... objects) {
      logger.warn(s, objects);
    }

    public void warn(String s, Object o, Object o1) {
      logger.warn(s, o, o1);
    }

    public void warn(String s, Throwable throwable) {
      logger.warn(s, throwable);
    }

    public void error(String s) {
      logger.error(s);
    }

    public void error(String s, Object o) {
      logger.error(s, o);
    }

    public void error(String s, Object o, Object o1) {
      logger.error(s, o, o1);
    }

    public void error(String s, Object... objects) {
      logger.error(s, objects);
    }

    public void error(String s, Throwable throwable) {
      logger.error(s, throwable);
    }
  }
}
