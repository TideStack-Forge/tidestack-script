package com.ouroboros.script.intelligence.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ouroboros.script.ScriptContextContributor;
import com.ouroboros.script.intelligence.ScriptContextCatalog;
import com.ouroboros.util.SPI;

public class SpiScriptContextCatalog implements ScriptContextCatalog {
  @Override
  public Map<String, Object> getGlobals() {
    var globals = new LinkedHashMap<String, Object>();
    SPI.getCachedReversedServiceStream(ScriptContextContributor.class)
        .map(ScriptContextContributor::getBindings)
        .forEach(globals::putAll);
    return globals;
  }
}
