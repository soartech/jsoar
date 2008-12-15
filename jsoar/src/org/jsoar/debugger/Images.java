/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger;

import java.net.URL;

import javax.swing.ImageIcon;

/**
 * @author ray
 */
public class Images
{
    public static final ImageIcon PAUSE = loadImage("/org/jsoar/debugger/pause.gif");
    public static final ImageIcon START = loadImage("/org/jsoar/debugger/start.gif");
    public static final ImageIcon STOP = loadImage("/org/jsoar/debugger/stop.gif");
    public static final ImageIcon REFRESH = loadImage("/org/jsoar/debugger/refresh.gif");
    public static final ImageIcon UNDO = loadImage("/org/jsoar/debugger/undo.gif");
    public static final ImageIcon WME = loadImage("/org/jsoar/debugger/wme.gif");
    public static final ImageIcon ID = loadImage("/org/jsoar/debugger/id.gif");
    public static final ImageIcon PRODUCTION = loadImage("/org/jsoar/debugger/production.gif");
    public static final ImageIcon SYNCH = loadImage("/org/jsoar/debugger/synch.gif");
    public static final ImageIcon DELETE = loadImage("/org/jsoar/debugger/delete.gif");
    public static final ImageIcon NEXT = loadImage("/org/jsoar/debugger/next.gif");
    public static final ImageIcon PREVIOUS = loadImage("/org/jsoar/debugger/previous.gif");
    public static final ImageIcon COPY = loadImage("/org/jsoar/debugger/copy.gif");
    public static final ImageIcon CLEAR = loadImage("/org/jsoar/debugger/clear.gif");
    public static final ImageIcon EDIT = loadImage("/org/jsoar/debugger/edit.gif");

    /**
     * @param string
     * @return
     */
    private static ImageIcon loadImage(String file)
    {
        URL url = Images.class.getResource(file);
        return new ImageIcon(url);
    }
}
