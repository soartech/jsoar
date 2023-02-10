/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 26, 2010
 */
package org.jsoar.kernel.modules;

import com.google.common.base.Predicate;

/**
 * @author ray
 */
public class PrimitiveParam implements Param
{
    private final String name;
    private String value;
    private final Predicate<String> val_pred;
    private final Predicate<String> prot_pred;
    
    public PrimitiveParam(String name, String value, Predicate<String> valPred,
            Predicate<String> protPred)
    {
        this.name = name;
        this.value = value;
        val_pred = valPred;
        prot_pred = protPred;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.modules.Param#get_name()
     */
    @Override
    public String get_name()
    {
        return name;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.modules.Param#set_string(java.lang.String)
     */
    @Override
    public boolean set_string(String newString)
    {
        if(!val_pred.apply(newString) || prot_pred.apply(newString))
            return false;
        
        set_value(newString);
        return true;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.modules.Param#validate_string(java.lang.String)
     */
    @Override
    public boolean validate_string(String newString)
    {
        return val_pred.apply(newString);
    }
    
    public void set_value(String newString)
    {
        value = newString;
    }
    
    public String get_value()
    {
        return value;
    }
}
