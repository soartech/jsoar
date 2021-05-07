/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jsoar.util.properties.PropertyKey;

/** @author ray */
public class PropertiesResource extends BaseAgentResource {
  /* (non-Javadoc)
   * @see org.jsoar.legilimens.resources.BaseAgentResource#setTemplateAttributes(java.util.Map)
   */
  @Override
  public void setTemplateAttributes(Map<String, Object> attrs) {
    super.setTemplateAttributes(attrs);

    final List<Property> props = new ArrayList<Property>();
    final List<PropertyKey<?>> keys = agent.getProperties().getKeys();
    Collections.sort(keys, PropertyKey.NAME_COMPARATOR);
    for (PropertyKey<?> key : keys) {
      final Object value = agent.getProperties().get(key);
      props.add(new Property(key, value));
    }
    attrs.put("properties", props);
  }

  public static class Property {
    public final PropertyKey<?> key;
    public final Object value;

    public Property(PropertyKey<?> key, Object value) {
      this.key = key;
      this.value = value;
    }

    /** @return the key */
    public PropertyKey<?> getKey() {
      return key;
    }

    /** @return the value */
    public String getValue() {
      return value != null ? value.toString() : "null";
    }
  }
}
