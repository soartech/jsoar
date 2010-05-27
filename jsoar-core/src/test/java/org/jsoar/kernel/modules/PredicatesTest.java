/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 26, 2010
 */
package org.jsoar.kernel.modules;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.base.Predicate;
import static org.jsoar.kernel.modules.Predicates.*;


/**
 * @author ray
 */
public class PredicatesTest
{
    @Test
    public void testBetweenDoubleInclusivePredicate()
    {
        final Predicate<Double> btw = betweenDouble(-3.0, 5.0, true);
        assertTrue(btw.apply(-3.0));
        assertTrue(btw.apply(5.0));
        assertTrue(btw.apply(1.0));
        assertFalse(btw.apply(-3.1));
        assertFalse(btw.apply(5.1));
    }
    
    @Test
    public void testBetweenDoubleNonInclusivePredicate()
    {
        final Predicate<Double> btw = betweenDouble(-3.0, 5.0, false);
        assertFalse(btw.apply(-3.0));
        assertFalse(btw.apply(5.0));
        assertTrue(btw.apply(-2.9));
        assertTrue(btw.apply(4.9));
        assertTrue(btw.apply(1.0));
        assertFalse(btw.apply(-3.1));
        assertFalse(btw.apply(5.1));
    }
    
    @Test
    public void testGreaterThanDoubleInclusivePredicate()
    {
        final Predicate<Double> btw = greaterThanDouble(-3.0, true);
        assertTrue(btw.apply(-3.0));
        assertTrue(btw.apply(500.0));
        assertTrue(btw.apply(1.0));
        assertFalse(btw.apply(-3.1));
        assertTrue(btw.apply(5.1));
    }
    
    @Test
    public void testGreaterThanDoubleNonInclusivePredicate()
    {
        final Predicate<Double> btw = greaterThanDouble(-3.0, false);
        assertFalse(btw.apply(-3.0));
        assertTrue(btw.apply(500.0));
        assertTrue(btw.apply(1.0));
        assertFalse(btw.apply(-3.1));
        assertTrue(btw.apply(5.1));
    }
    
    @Test
    public void testLessThanDoubleInclusivePredicate()
    {
        final Predicate<Double> btw = lessThanDouble(-3.0, true);
        assertTrue(btw.apply(-3.0));
        assertFalse(btw.apply(500.0));
        assertFalse(btw.apply(1.0));
        assertTrue(btw.apply(-3.1));
        assertFalse(btw.apply(5.1));
    }
    
    @Test
    public void testLessThanDoubleNonInclusivePredicate()
    {
        final Predicate<Double> btw = lessThanDouble(-3.0, false);
        assertFalse(btw.apply(-3.0));
        assertFalse(btw.apply(500.0));
        assertFalse(btw.apply(1.0));
        assertTrue(btw.apply(-3.1));
        assertFalse(btw.apply(5.1));
    }
    
    @Test
    public void testBetweenIntegerInclusivePredicate()
    {
        final Predicate<Integer> btw = betweenInteger(-3, 5, true);
        assertTrue(btw.apply(-3));
        assertTrue(btw.apply(5));
        assertTrue(btw.apply(1));
        assertFalse(btw.apply(-4));
        assertFalse(btw.apply(6));
    }
    
    @Test
    public void testBetweenIntegerNonInclusivePredicate()
    {
        final Predicate<Integer> btw = betweenInteger(-3, 5, false);
        assertFalse(btw.apply(-3));
        assertFalse(btw.apply(5));
        assertTrue(btw.apply(-2));
        assertTrue(btw.apply(4));
        assertTrue(btw.apply(1));
        assertFalse(btw.apply(-4));
        assertFalse(btw.apply(6));
    }
    
    @Test
    public void testGreaterThanIntegerInclusivePredicate()
    {
        final Predicate<Integer> btw = greaterThanInteger(-3, true);
        assertTrue(btw.apply(-3));
        assertTrue(btw.apply(500));
        assertTrue(btw.apply(1));
        assertFalse(btw.apply(-4));
        assertTrue(btw.apply(6));
    }
    
    @Test
    public void testGreaterThanIntegerNonInclusivePredicate()
    {
        final Predicate<Integer> btw = greaterThanInteger(-3, false);
        assertFalse(btw.apply(-3));
        assertTrue(btw.apply(500));
        assertTrue(btw.apply(1));
        assertFalse(btw.apply(-4));
        assertTrue(btw.apply(6));
    }
    
    @Test
    public void testLessThanIntegerInclusivePredicate()
    {
        final Predicate<Integer> btw = lessThanInteger(-3, true);
        assertTrue(btw.apply(-3));
        assertFalse(btw.apply(500));
        assertFalse(btw.apply(1));
        assertTrue(btw.apply(-4));
        assertFalse(btw.apply(6));
    }
    
    @Test
    public void testLessThanIntegerNonInclusivePredicate()
    {
        final Predicate<Integer> btw = lessThanInteger(-3, false);
        assertFalse(btw.apply(-3));
        assertFalse(btw.apply(500));
        assertFalse(btw.apply(1));
        assertTrue(btw.apply(-4));
        assertFalse(btw.apply(6));
    }
    
}
