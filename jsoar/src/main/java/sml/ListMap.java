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
import java.util.concurrent.CopyOnWriteArrayList;

import org.jsoar.util.ByRef;

/**
 * @author ray
 */
public class ListMap<KeyType, ValueType>
{
    protected Map<KeyType, List<ValueType>>  m_Map = new TreeMap<KeyType, List<ValueType>>();

    public static <K, V> ListMap<K, V> newInstance()
    {
        return new ListMap<K, V>();
    }
    
    public ListMap() { } 

    public void clear()
     {
         m_Map.clear() ;
     }

    public void add(KeyType key, ValueType value, boolean addToBack)
     {
         // See if we already have a list
        List<ValueType> pList = getList(key) ;

         // If not, create one
         if (pList == null)
         {
             pList = new CopyOnWriteArrayList<ValueType>() ;
             m_Map.put(key, pList);
         }

         // Add this item to the list
         if (addToBack)
             pList.add(value) ;
         else
             pList.add(0, value) ;
     }

     // Remove a specific value from the "key" list
     public void remove(KeyType key, ValueType value)
     {
         List<ValueType> pList = getList(key) ;

         if (pList != null)
             pList.remove(value) ;
     }

     // Search all values and remove the given one(s)
     // The way we identify which one(s) to remove is by passing in an object which implements "isEqual".
     // When it's true we remove the object.  Returns true if at least one object was removed.
     public boolean removeAllByTest(ListMapValueTest<ValueType> pTest)
     {
         boolean removedAnObject = false ;

         for(List<ValueType> pList : m_Map.values())
         {
             // Walk the list, removing items based on if they match the test
             Iterator<ValueType> it = pList.iterator();
             while(it.hasNext())
             {
                 ValueType value = it.next();
                 
                 if(pTest.isEqual(value))
                 {
                     removedAnObject = true;
                     it.remove();
                 }
             }
         }
         return removedAnObject;
     }

     // Search all values to find a matching value and return the key for that value.
     // The "match" is based on implementing "isEqual" in a ValueTest class.
     // The first match is returned.
     // If there is no match, returns "notFoundKey" that you pass in (so you can choose an appropriate value that's not a key)
     public KeyType findFirstKeyByTest(ListMapValueTest<ValueType> pTest, KeyType notFoundKey)
     {
         for(Map.Entry<KeyType, List<ValueType>> e : m_Map.entrySet())
         {
             KeyType key = e.getKey();
             
             for(ValueType v : e.getValue())
             {
                 if(pTest.isEqual(v))
                 {
                     return key;
                 }
             }
         }
         return notFoundKey;
     }

     // Search all values to find the first value that matches this test.
     public boolean findFirstValueByTest(ListMapValueTest<ValueType> pTest, ByRef<ValueType> pReturnValue)
     {
         for(List<ValueType> pList : m_Map.values())
         {
             // Walk the list, removing items based on if they match the test
             Iterator<ValueType> it = pList.iterator();
             while(it.hasNext())
             {
                 ValueType value = it.next();
                 
                 if(pTest.isEqual(value))
                 {
                     pReturnValue.value = value;
                     return true;
                 }
             }
         }
         return false;
     }


     // Remove all values for a specific key
     public void removeAll(KeyType key)
     {
         List<ValueType> pList = getList(key) ;

         if (pList != null)
             pList.clear() ;
     }

     // Get the number of items in a specific list
     public int getListSize(KeyType key)
     {
         List<ValueType> pList = getList(key) ;

         // There is no list for this key at all
         if (pList == null)
             return 0 ;

         return pList.size() ;
     }

     // Returns the list of items associated with the key (can be NULL)
     public List<ValueType>  getList(KeyType key)
     {
         return m_Map.get(key);
     }

     public int getSize()
     {
         return (int)m_Map.size() ;
     }

}
