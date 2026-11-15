package com.ouroboros.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ContextWrappingHelperTest {

  @Test
  public void exposesMergedReadOnlyMapView() {
    Map<String, Object> wrapped = new HashMap<String, Object>();
    wrapped.put("wrappedOnly", 1);
    wrapped.put("shared", "wrapped");

    Map<String, Object> wrapper = new HashMap<String, Object>();
    wrapper.put("wrapperOnly", 2);
    wrapper.put("shared", "wrapper");

    ContextWrappingHelper helper = new ContextWrappingHelper(wrapped, wrapper);

    assertEquals(4, helper.size());
    assertFalse(helper.isEmpty());
    assertTrue(helper.containsKey("wrappedOnly"));
    assertTrue(helper.containsKey("wrapperOnly"));
    assertTrue(helper.containsValue(1));
    assertTrue(helper.containsValue(2));
    assertEquals("wrapper", helper.get("shared"));
    assertEquals(1, helper.get("wrappedOnly"));

    Set<String> keys = helper.keySet();
    assertEquals(new HashSet<>(Arrays.asList("wrappedOnly", "wrapperOnly", "shared")), keys);

    Collection<Object> values = helper.values();
    assertTrue(values.containsAll(Arrays.asList(1, 2, "wrapped", "wrapper")));

    Set<Map.Entry<String, Object>> entries = helper.entrySet();
    assertTrue(entries.stream().anyMatch(e -> e.getKey().equals("wrappedOnly") && e.getValue().equals(1)));
    assertTrue(entries.stream().anyMatch(e -> e.getKey().equals("wrapperOnly") && e.getValue().equals(2)));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void putIsUnsupported() {
    new ContextWrappingHelper(Collections.emptyMap(), Collections.emptyMap()).put("k", "v");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void removeIsUnsupported() {
    new ContextWrappingHelper(Collections.emptyMap(), Collections.singletonMap("k", "v")).remove("k");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void putAllIsUnsupported() {
    new ContextWrappingHelper(Collections.emptyMap(), Collections.emptyMap()).putAll(Collections.singletonMap("k", "v"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void clearIsUnsupported() {
    new ContextWrappingHelper(Collections.emptyMap(), Collections.emptyMap()).clear();
  }

  @Test
  public void nullWrappedMapFallsBackToEmptyMap() {
    ContextWrappingHelper helper = new ContextWrappingHelper(null, Collections.<String, Object>singletonMap("wrapper", 1));

    assertEquals(1, helper.size());
    assertTrue(helper.containsKey("wrapper"));
    assertEquals(1, helper.get("wrapper"));
  }
}
