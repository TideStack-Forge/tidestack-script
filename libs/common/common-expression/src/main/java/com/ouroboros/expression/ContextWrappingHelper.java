package com.ouroboros.expression;


import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContextWrappingHelper implements Map<String, Object> {
  private final Map<String, Object> wrapped;
  private final Map<String, Object> wrapper;

  public ContextWrappingHelper(Map<String, Object> wrappedMap, Map<String, Object> wrapperMap) {
    this.wrapped = wrappedMap == null ? Collections.emptyMap() : wrappedMap;
    this.wrapper = wrapperMap;
  }

  @Override
  public int size() {
    return wrapper.size() + wrapped.size();
  }

  @Override
  public boolean isEmpty() {
    return wrapper.isEmpty() && wrapped.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return wrapper.containsKey(key) || wrapped.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return wrapper.containsValue(value) || wrapped.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return wrapper.getOrDefault(key, wrapped.get(key));
  }

  @Override
  public Set<String> keySet() {
    return Stream.concat(wrapper.keySet().stream(), wrapped.keySet().stream())
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<Object> values() {
    return Stream.concat(wrapper.values().stream(), wrapped.values().stream())
        .collect(Collectors.toList());
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return Stream.concat(wrapper.entrySet().stream(), wrapped.entrySet().stream())
        .collect(Collectors.toSet());
  }

  @Override
  public Object put(String key, Object value) {
    throw new UnsupportedOperationException("上下文不允许被修改");
  }

  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException("上下文不允许被修改");
  }

  @Override
  public void putAll(Map<? extends String, ?> m) {
    throw new UnsupportedOperationException("上下文不允许被修改");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("上下文不允许被修改");
  }
}
