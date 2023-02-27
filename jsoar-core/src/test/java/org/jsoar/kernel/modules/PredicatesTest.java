/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 26, 2010
 */
package org.jsoar.kernel.modules;

import static org.jsoar.kernel.modules.Predicates.betweenDouble;
import static org.jsoar.kernel.modules.Predicates.betweenInteger;
import static org.jsoar.kernel.modules.Predicates.greaterThanDouble;
import static org.jsoar.kernel.modules.Predicates.greaterThanInteger;
import static org.jsoar.kernel.modules.Predicates.lessThanDouble;
import static org.jsoar.kernel.modules.Predicates.lessThanInteger;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.common.base.Predicate;

/**
 * @author ray
 */
class PredicatesTest
{
    @Test
    void testBetweenDoubleInclusivePredicate()
    {
        final Predicate<Double> btw = betweenDouble(-3.0, 5.0, true);
        assertTrue(btw.apply(-3.0));
        assertTrue(btw.apply(5.0));
        assertTrue(btw.apply(1.0));
        assertFalse(btw.apply(-3.1));
        assertFalse(btw.apply(5.1));
    }
    
    @Test
    void testBetweenDoubleNonInclusivePredicate()
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
    void testGreaterThanDoubleInclusivePredicate()
    {
        final Predicate<Double> btw = greaterThanDouble(-3.0, true);
        assertTrue(btw.apply(-3.0));
        assertTrue(btw.apply(500.0));
        assertTrue(btw.apply(1.0));
        assertFalse(btw.apply(-3.1));
        assertTrue(btw.apply(5.1));
    }
    
    @Test
    void testGreaterThanDoubleNonInclusivePredicate()
    {
        final Predicate<Double> btw = greaterThanDouble(-3.0, false);
        assertFalse(btw.apply(-3.0));
        assertTrue(btw.apply(500.0));
        assertTrue(btw.apply(1.0));
        assertFalse(btw.apply(-3.1));
        assertTrue(btw.apply(5.1));
    }
    
    @Test
    void testLessThanDoubleInclusivePredicate()
    {
        final Predicate<Double> btw = lessThanDouble(-3.0, true);
        assertTrue(btw.apply(-3.0));
        assertFalse(btw.apply(500.0));
        assertFalse(btw.apply(1.0));
        assertTrue(btw.apply(-3.1));
        assertFalse(btw.apply(5.1));
    }
    
    @Test
    void testLessThanDoubleNonInclusivePredicate()
    {
        final Predicate<Double> btw = lessThanDouble(-3.0, false);
        assertFalse(btw.apply(-3.0));
        assertFalse(btw.apply(500.0));
        assertFalse(btw.apply(1.0));
        assertTrue(btw.apply(-3.1));
        assertFalse(btw.apply(5.1));
    }
    
    @Test
    void testBetweenIntegerInclusivePredicate()
    {
        final Predicate<Integer> btw = betweenInteger(-3, 5, true);
        assertTrue(btw.apply(-3));
        assertTrue(btw.apply(5));
        assertTrue(btw.apply(1));
        assertFalse(btw.apply(-4));
        assertFalse(btw.apply(6));
    }
    
    @Test
    void testBetweenIntegerNonInclusivePredicate()
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
    void testGreaterThanIntegerInclusivePredicate()
    {
        final Predicate<Integer> btw = greaterThanInteger(-3, true);
        assertTrue(btw.apply(-3));
        assertTrue(btw.apply(500));
        assertTrue(btw.apply(1));
        assertFalse(btw.apply(-4));
        assertTrue(btw.apply(6));
    }
    
    @Test
    void testGreaterThanIntegerNonInclusivePredicate()
    {
        final Predicate<Integer> btw = greaterThanInteger(-3, false);
        assertFalse(btw.apply(-3));
        assertTrue(btw.apply(500));
        assertTrue(btw.apply(1));
        assertFalse(btw.apply(-4));
        assertTrue(btw.apply(6));
    }
    
    @Test
    void testLessThanIntegerInclusivePredicate()
    {
        final Predicate<Integer> btw = lessThanInteger(-3, true);
        assertTrue(btw.apply(-3));
        assertFalse(btw.apply(500));
        assertFalse(btw.apply(1));
        assertTrue(btw.apply(-4));
        assertFalse(btw.apply(6));
    }
    
    @Test
    void testLessThanIntegerNonInclusivePredicate()
    {
        final Predicate<Integer> btw = lessThanInteger(-3, false);
        assertFalse(btw.apply(-3));
        assertFalse(btw.apply(500));
        assertFalse(btw.apply(1));
        assertTrue(btw.apply(-4));
        assertFalse(btw.apply(6));
    }
    
}
