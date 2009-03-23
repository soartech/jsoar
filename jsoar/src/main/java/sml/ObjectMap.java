/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 15, 2008
 */
package sml;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author ray
 */
public class ObjectMap<DataType>
{
    private Map<String, DataType> m_Map = new TreeMap<String, DataType>();
    
    public ObjectMap() { } 

    public void clear()
    {
        // TODO: ObjectMap: Call delete()?
        // Delete the contents of the map
//        for(InternalMapIter mapIter = m_Map.begin(); mapIter != m_Map.end(); ++mapIter)
//        {
//            DataType pObject = mapIter->second ;
//
//            // We only delete the object.  The name is a pointer
//            // into the object structure, so it's deleted when the object is deleted.
//            delete pObject ;
//        }
        m_Map.clear() ;
    }

    public int size() { return m_Map.size() ; }

    public boolean contains(DataType value)
    {
        return m_Map.values().contains(value);
    }

    public DataType getIndex(int index)
    {
        for(DataType value : m_Map.values())
        {
            if (index == 0)
                return value;
            index-- ;
        }

        return null;
    }

    public void add(String pName, DataType pObject)
    {
        // If we already have an object registered with this name delete it.
        // Otherwise we'll have a memory leak.
        remove(pName) ;

        m_Map.put(pName, pObject);
    }

    // Remove all agents not on this list
    public void keep(List<DataType> pKeepList)
    {
        Iterator<DataType> it = m_Map.values().iterator();
        while(it.hasNext())
        {
            DataType pObject = it.next();

            // For lists, you have to use std::find, they don't have a member function
            if (!pKeepList.contains(pObject))
                it.remove();
        }
    }

    public DataType find(String pName)
    {
        return m_Map.get(pName);
    }

    public boolean remove(String pName)
    {
        return remove(pName, true);
    }
    
    public boolean remove(String pName, boolean deleteObject /*= true*/)
    {
        DataType pObject = m_Map.remove(pName);
        if(pObject == null)
        {
            return false;
        }

        if (deleteObject)
        {
            // TODO: ObjectMap: call delete()?
        }

        return true ;
    }

}
