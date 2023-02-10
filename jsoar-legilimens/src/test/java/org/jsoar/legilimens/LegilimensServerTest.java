/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2009
 */
package org.jsoar.legilimens;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.jsoar.runtime.LegilimensStarter;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class LegilimensServerTest
{
    @Test
    public void testThatTheNameOfTheServerHasntChanged()
    {
        // The server is invoked reflectively from org.jsoar.runtime.LegilimensStarter
        // so if this test fails then we'll be reminded to go fix references there.
        
        final Class<?> serverClass = LegilimensStarter.getServerClass();
        assertNotNull(serverClass);
        assertSame(LegilimensServer.class, serverClass);
        assertNotNull(LegilimensStarter.getStartMethod(serverClass));
    }
}
