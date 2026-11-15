package com.ouroboros.script;

import org.graalvm.polyglot.HostAccess;

public interface JsHostAccessConfigurer {
  void config(HostAccess.Builder builder);
}
