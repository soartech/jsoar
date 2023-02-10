/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 26, 2010
 */
package org.jsoar.kernel.modules;

/**
 * @author ray
 */
public interface Param
{
    
    String get_name();
    
    boolean set_string(String new_string);
    
    boolean validate_string(String new_string);
}
