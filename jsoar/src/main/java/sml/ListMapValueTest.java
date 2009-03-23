/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 15, 2008
 */
package sml;

/**
 *  // In order to do a deletion or lookup using a test on a value
 // derive a class from this and implement "isEqual".
 // (We use this so we can extract a value from the ValueType and test that
 //  without having to rebuild the complete ValueType.  In practice this is helpful).

 * @author ray
 */
public interface ListMapValueTest<ValueType>
{
    public boolean isEqual(ValueType value);
}
