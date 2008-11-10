/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 6, 2008
 */
package sml.connection;

/**
 * @author ray
 */
public interface ListenerCallback
{
    void execute(Connection connection, Object user);
}
