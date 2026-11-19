package com.ouroboros.expression.impl;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.ouroboros.expression.EL;

import static org.junit.jupiter.api.Assertions.*;

public class SpElExpressionPlanTest {
  @Test
  public void selectorPresenceDistinguishesMissingNullAndFixedIndexes() {
    var body = new LinkedHashMap<String, Object>();
    body.put("comment", null);
    body.put("lines", List.of(Map.of("id", 7)));
    var values = Map.<String, Object>of("request", Map.of("body", body));
    var comment = EL.compile("${ request.body.comment }").get();
    assertTrue(comment.isPresent(values).get());
    assertNull(comment.evaluate(values).get());
    assertFalse(EL.compile("${ request.body.missing }").get().isPresent(values).get());
    assertFalse(EL.compile("${ request.body.comment.text }").get().isPresent(values).get());
    var item = EL.compile("${ request.body.lines[0]['id'] }").get();
    assertEquals(List.of("request", "body", "lines", 0, "id"), item.selectorPath().orElseThrow());
    assertTrue(item.isPresent(values).get());
    assertEquals(7, item.evaluate(values).get());
    assertFalse(EL.compile("${ request.body.lines[1].id }").get().isPresent(values).get());
  }

  @Test
  public void computationsAreDeclaredPresentAndAreNotEvaluatedByPresenceChecksOrCompilation() {
    var reads = new AtomicInteger();
    Map<String, Object> lazy = new AbstractMap<>() {
      @Override public Object get(Object key) { reads.incrementAndGet(); return null; }
      @Override public boolean containsKey(Object key) { return false; }
      @Override public Set<Entry<String, Object>> entrySet() { throw new AssertionError("Must not enumerate values"); }
    };
    var plan = EL.compile("${ request.limit ?: 20 }").get();
    assertTrue(plan.selectorPath().isEmpty());
    assertTrue(plan.isPresent(Map.of("request", lazy)).get());
    assertEquals(0, reads.get());
    assertEquals(20, plan.evaluate(Map.of("request", lazy)).get());
    assertTrue(reads.get() > 0);
    var nullPlan = EL.compile("${ null }").get();
    assertTrue(nullPlan.isPresent(Map.of()).get());
    assertNull(nullPlan.evaluate(Map.of()).get());
  }

  @Test
  public void selectorPresenceDoesNotReadTheFinalValueAndDoesNotEnumerateLazyMaps() {
    var reads = new AtomicInteger();
    Map<String, Object> lazy = new AbstractMap<>() {
      @Override public Object get(Object key) { reads.incrementAndGet(); return "value"; }
      @Override public boolean containsKey(Object key) { return key.equals("x-token"); }
      @Override public Set<Entry<String, Object>> entrySet() { throw new AssertionError("Must not enumerate headers"); }
    };
    var plan = EL.compile("${ request.params['x-token'] }").get();
    var values = Map.<String, Object>of("request", Map.of("params", lazy));
    assertTrue(plan.isPresent(values).get());
    assertEquals(0, reads.get());
    assertEquals("value", plan.evaluate(values).get());
    assertEquals(1, reads.get());
  }

  @Test
  public void compiledValuesPreserveTypesReferencesTemplatesEscapesAndEngineErrors() {
    var data = new LinkedHashMap<String, Object>();
    var values = Map.<String, Object>of("data", data, "input", Map.of("params", Map.of("records", List.of(Map.of("id", 3), Map.of()))));
    assertSame(data, EL.compile("${ data }").get().evaluate(values).get());
    assertEquals(java.util.Arrays.asList(3, null), EL.compile("${ input.params.records?.![id] }").get().evaluate(values).get());
    assertNull(EL.compile("${ input.params.records?.![id] }").get().evaluate(Map.of("input", Map.of("params", Map.of()))).get());
    assertEquals("item-3-${literal}", EL.compile("item-${ 1 + 2 }-$${literal}").get().evaluate(values).get());
    assertTrue(EL.compile("${ broken[ }").isLeft());
    assertTrue(EL.compile("${ 1 / 0 }").get().evaluate(Map.of()).isLeft());
    assertEquals("plain", EL.compile("plain").get().evaluate(Map.of()).get());
  }

  @Test
  public void theSamePlanCanBeReusedWithIndependentInvocationsAndFlatKeys() {
    var plan = EL.compile("${ input.value }").get();
    assertFalse(plan.isPresent(Map.of("input", Map.of())).get());
    assertNull(plan.evaluate(Map.of("input", Map.of())).get());
    assertTrue(plan.isPresent(Map.of("input.value", 4)).get());
    assertEquals(4, plan.evaluate(Map.of("input.value", 4)).get());
    assertEquals(9, plan.evaluate(Map.of("input", Map.of("value", 9))).get());
    assertNull(plan.evaluate(Map.of("input", Collections.singletonMap("value", null))).get());
  }
}
