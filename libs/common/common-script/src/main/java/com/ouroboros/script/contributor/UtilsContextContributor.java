package com.ouroboros.script.contributor;

import java.util.HashMap;
import java.util.Map;

import com.ouroboros.script.ScriptContextContributor;
import com.ouroboros.util.Converter;
import com.ouroboros.util.Dates;
import com.ouroboros.util.Maps;

public class UtilsContextContributor implements ScriptContextContributor {
  @Override
  public Map<String, Object> getBindings() {
    var map = new HashMap<String, Object>();
    map.put("Dates", Dates.class);
    map.put("Converter", Converter.class);
    map.put("Maps", Maps.class);
    return map;
  }
}
