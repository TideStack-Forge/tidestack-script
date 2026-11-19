package com.ouroboros.expression.impl;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SpElRestrictedPredicateTest {
  @Test
  public void predicatesReadOnlyTheProvidedJsonValues() {
    var values = Map.<String, Object>of("event", Map.of("payload", Map.of("amount", 12, "state", "submitted")));
    var engine = new SpElImpl();
    assertTrue(engine.evaluatePredicate("${event.payload.amount > 10 and event.payload['state'] == 'submitted'}", values).get());
    assertFalse(engine.evaluatePredicate("${event.payload.amount < 10}", values).get());
    assertTrue(engine.evaluatePredicate("${utils == null}", values).get());
    assertTrue(engine.evaluatePredicate("${event.payload.missing == null}", values).get());
    assertTrue(engine.evaluatePredicate("${event.payload.amount}", values).isLeft());
  }

  @Test
  public void validationRejectsForbiddenNodesEvenInsideAnUnreachableBranch() {
    var engine = new SpElImpl();
    for (var expression : List.of("${T(java.lang.System).getProperties() != null}",
        "${false and T(java.lang.System).setProperty('probe', 'changed') == null}",
        "${event.payload.clear() == null}", "${event.payload.amount = 0}",
        "${new java.util.Date() != null}", "${#external != null}", "prefix ${true}")) {
      assertTrue(engine.validatePredicateExpression(expression).isLeft(), expression);
      assertTrue(engine.evaluatePredicate(expression, Map.of("event", Map.of("payload", Map.of("amount", 1)))).isLeft(), expression);
    }
    assertTrue(engine.validatePredicateExpression("${event.payload.amount >= 1}").get());
  }
  @Test
  public void compiledPredicatesExposeAllReadsAndRestrictRootsAndDynamicIndexes() {
    var engine = new SpElImpl();
    var compiled = engine.compilePredicateExpression(
        "${input.params.amount > 0 and objectStates['/params/orders/0'].state == 'DRAFT'}",
        java.util.Set.of("input", "objectStates")).get();
    assertEquals(List.of(List.of("input", "params", "amount"), List.of("objectStates", "/params/orders/0", "state")), compiled.accessPaths());
    assertTrue(compiled.evaluate(Map.of("input", Map.of("params", Map.of("amount", 2)),
        "objectStates", Map.of("/params/orders/0", Map.of("state", "DRAFT")))).get());
    for (String invalid : List.of("${false and request == null}", "${utils == null}",
        "${objectStates[input.params.role].state == 'DRAFT'}", "${@service != null}")) {
      assertTrue(engine.compilePredicateExpression(invalid, java.util.Set.of("input", "objectStates")).isLeft(), invalid);
    }
  }

}
