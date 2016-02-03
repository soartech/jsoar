package org.jsoar.kernel.io.json;

import com.google.common.collect.Maps;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Iterator;
import java.util.Map;

/**
 * Utility methods for adding json to the input link and converting output link wmes to json.
 * 
 * @author patrick.dehaan
 * @author chris.kawatsu
 *
 */
public class JsonWmeUtils
{
    private JsonWmeUtils() {}

    public static void addWmes(
            InputOutput io,
            Object attr,
            Object obj)
    {
        if (obj instanceof JSONObject)
        {
            addObjectWmes(io, attr, (JSONObject) obj);
        }
        else if (obj instanceof JSONArray)
        {
            addArrayWmes(io, attr, (JSONArray) obj);
        }
        else if (obj instanceof Boolean)
        {
            InputWmes.add(io, attr, Boolean.toString((Boolean)obj));
        }
        else
        {
            InputWmes.add(io, attr, obj);
        }
    }

    public static void addWmes(
            InputOutput io,
            Identifier id,
            Object attr,
            Object obj)
    {
        if (obj instanceof JSONObject)
        {
            addObjectWmes(io, id, attr, (JSONObject) obj);
        }
        else if (obj instanceof JSONArray)
        {
            addArrayWmes(io, id, attr, (JSONArray) obj);
        }
        else if (obj instanceof Boolean)
        {
            InputWmes.add(io, id, attr, Boolean.toString((Boolean)obj));
        }
        else
        {
            InputWmes.add(io, id, attr, obj);
        }
    }

    public static void addWmes(
            InputWme root,
            Object attr,
            Object obj)
    {
        if (obj instanceof JSONObject)
        {
            addObjectWmes(root, attr, (JSONObject) obj);
        }
        else if (obj instanceof JSONArray)
        {
            addArrayWmes(root, attr, (JSONArray) obj);
        }
        else if (obj instanceof Boolean)
        {
            InputWmes.add(root, attr, Boolean.toString((Boolean)obj));
        }
        else
        {
            InputWmes.add(root, attr, obj);
        }
    }

    @SuppressWarnings("rawtypes")
    public static void addObjectWmes(
            InputOutput io,
            Object attr,
            JSONObject jsonObj)
    {
        InputWme wme = InputWmes.add(io, attr, Symbols.NEW_ID);
        Iterator iter = jsonObj.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry entry = (Map.Entry) iter.next();
            addWmes(wme, entry.getKey().toString(), entry.getValue());
        }
    }

    @SuppressWarnings("rawtypes")
    public static void addObjectWmes(
            InputOutput io,
            Identifier id,
            Object attr,
            JSONObject jsonObj)
    {
        InputWme wme = InputWmes.add(io, id, attr, Symbols.NEW_ID);
        Iterator iter = jsonObj.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry entry = (Map.Entry) iter.next();
            addWmes(wme, entry.getKey().toString(), entry.getValue());
        }
    }

    public static void addObjectWmes(
            InputWme root,
            Object attr,
            JSONObject jsonObj)
    {
        InputWme wme = InputWmes.add(root, attr, Symbols.NEW_ID);
        addObjectWmes(wme, jsonObj);
    }
    
    @SuppressWarnings("rawtypes")
    public static void addObjectWmes(
            InputWme root,
            JSONObject jsonObj)
    {
        Iterator iter = jsonObj.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry entry = (Map.Entry) iter.next();
            addWmes(root, entry.getKey().toString(), entry.getValue());
        }
    }

    public static void addArrayWmes(
            InputOutput io,
            Object attr,
            JSONArray jsonArray)
    {
        for (Object obj : jsonArray)
        {
            addWmes(io, attr, obj);
        }
    }

    public static void addArrayWmes(
            InputOutput io,
            Identifier id,
            Object attr,
            JSONArray jsonArray)
    {
        for (Object obj : jsonArray)
        {
            addWmes(io, id, attr, obj);
        }
    }

    public static void addArrayWmes(
            InputWme root,
            Object attr,
            JSONArray jsonArray)
    {
        for (Object obj : jsonArray)
        {
            addWmes(root, attr, obj);
        }
    }

    public static Object parse(Symbol symbol)
    {
        if (symbol.asIdentifier() != null)
        {
            return parse(symbol.asIdentifier());
        }
        else if (symbol.asDouble() != null)
        {
            return symbol.asDouble().getValue();
        }
        else if (symbol.asInteger() != null)
        {
            return symbol.asInteger().getValue();
        }
        else
        {
            return symbol.toString();
        }
    }

    @SuppressWarnings("unchecked")
    public static JSONObject parse(Identifier root)
    {
        JSONObject jsonObj = new JSONObject();
        Map<String, JSONArray> arrayAttrs = Maps.newHashMap();

        Iterator<Wme> wmes = root.getWmes();
        while (wmes.hasNext())
        {
            Wme wme = wmes.next();

            Object jsonValue = parse(wme.getValue());

            String attr = wme.getAttribute().toString();

            if (jsonObj.containsKey(attr))
            {
                // collect multi-value attributes into an array
                JSONArray array = arrayAttrs.get(attr);
                if (array == null)
                {
                    // no array yet, create one and add the existing value
                    array = new JSONArray();
                    array.add(jsonObj.get(attr));
                    jsonObj.put(attr, array);
                    arrayAttrs.put(attr, array);
                }
                array.add(jsonValue);
            }
            else
            {
                jsonObj.put(attr, jsonValue);
            }
        }

        return jsonObj;
    }
}
