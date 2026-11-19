package com.ouroboros.expression.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.ouroboros.expression.EL;
import com.ouroboros.expression.ExpressionInputReference;

import static org.junit.jupiter.api.Assertions.*;

public class SpElInputAnalysisTest {
  @Test
  public void scopedCollectionsAreExplicitlyUnsupportedInsteadOfInventingExternalRoots() {
    for (var expression : List.of("${orders.![id]}", "${orders.?[id > 1]}", "${#root.order.id}", "${#this.id}")) {
      assertTrue(EL.compile(expression).get().inputReferences().isLeft(), expression);
    }
    assertEquals(List.of(1, 2), EL.compile("${orders.![id]}").get()
        .evaluate(Map.of("orders", List.of(Map.of("id", 1), Map.of("id", 2)))).get());
  }
  @Test
  public void arithmeticAndDynamicIndexesExposeRootsWithoutEvaluatingThem() {
    var plan = EL.compile("${orders[index].id + pageNo + 1}").get();
    var inputs = plan.inputReferences().get();
    assertEquals(Set.of("orders", "index", "pageNo"), inputs.stream()
        .map(ExpressionInputReference::root).collect(Collectors.toSet()));
    assertTrue(inputs.stream().filter(input -> input.root().equals("orders")).findFirst().orElseThrow().dynamic());
    assertEquals(8, plan.evaluate(Map.of("orders", List.of(Map.of("id", 5)), "index", 0, "pageNo", 2)).get());
  }

  @Test
  public void staticPathsAndTemplatePartsAreReportedWhileLiteralsAndMapKeysAreExcluded() {
    var inputs = EL.compile("item-${order.lines[0]['id']}-${{label: customer.name, fixed: 'literal'}}").get()
        .inputReferences().get();
    assertEquals(2, inputs.size());
    assertEquals(List.of("order", "lines", 0, "id"), inputs.getFirst().path());
    assertEquals("customer", inputs.getLast().root());
    assertTrue(EL.compile("${1 / 0}").get().inputReferences().get().isEmpty());
    assertTrue(EL.compile("plain-$${literal}").get().inputReferences().get().isEmpty());
    assertTrue(EL.compile("${broken[}").isLeft());
  }
}
