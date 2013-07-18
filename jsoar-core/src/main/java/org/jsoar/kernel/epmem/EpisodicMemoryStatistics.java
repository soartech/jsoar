/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 20, 2012
 */
package org.jsoar.kernel.epmem;

/**
 * @author voigtjr
 */
public interface EpisodicMemoryStatistics
{
    public long getTime();
    public void setTime(long time);
    public long getNextId();
    public void setNextId(long next_id);
}
