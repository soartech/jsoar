package org.jsoar.kernel.io.json;

import com.google.common.collect.Maps;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for adding json to the input link and converting output link wmes to json.
 * 
 * @author patrick.dehaan
 * @author chris.kawatsu
 *
 */
public class JsonWmeUtils
{
    private static Logger logger = LoggerFactory.getLogger(JsonWmeUtils.class);

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
    
    public static Object parse(Symbol symbol){
        return parse(symbol, false);
    }

    public static Object parse(Symbol symbol, Boolean autoCreateJsonArray)
    {
        if (symbol.asIdentifier() != null)
        {
            return parse(symbol.asIdentifier(), autoCreateJsonArray);
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
    public static JSONObject parse(Identifier root){
        return parse(root, false);
    }

    @SuppressWarnings("unchecked")
    public static JSONObject parse(Identifier root, Boolean autoCreateJSONArray)
    {
        final JSONObject jsonObj = new JSONObject();
        final Map<String, JSONArray> arrayAttrs = Maps.newHashMap();

        // Create empty arrays for all array attributes
        Iterator<String> arrayAttributes = getJsonArrayAttributes(root).iterator();
        while (arrayAttributes.hasNext())
        {
            String attr = arrayAttributes.next();
            JSONArray array = new JSONArray();
            jsonObj.put(attr, array);
            arrayAttrs.put(attr, array);
        }

        final Iterator<Wme> wmes = root.getWmes();
        while (wmes.hasNext())
        {
            final Wme wme = wmes.next();

            final String attr = wme.getAttribute().toString();

            // Ignore special attribute for setting JSON arrays.
            if(attr.equals(JSON_ARRAY))
            {
                continue;
            }

            // Recursively parse values.
            final Object jsonValue = parse(wme.getValue(), autoCreateJSONArray);

            // If this attribute is an array, then add the value to the array.
            // Otherwise, add it as a JSON value.
            // Multiple attributes that have not been specified as an array are considered an error.
            JSONArray array = arrayAttrs.get(attr);
            if (array != null)
            {
                array.add(jsonValue);
            }
            else
            {
                if (jsonObj.containsKey(attr))
                {
                    if(autoCreateJSONArray){
                        if(!(jsonObj.get(attr) instanceof JSONArray)){
                            Object oldObject = jsonObj.get(attr);
                            JSONArray newJsonArray = new JSONArray();
                            newJsonArray.add(oldObject);
                            newJsonArray.add(jsonValue);
                            jsonObj.put(attr, newJsonArray);
                        } else{
                            ((JSONArray) jsonObj.get(attr)).add(jsonValue);
                        }
                    }else{
                        logger.error("Parsing identifier {} - Replacing value for key {} ({} -> {}). To make an array instead add a ^{} {} WME to the identifier.",
                                     root, attr, jsonObj.get(attr), jsonValue, JSON_ARRAY, attr);
                        jsonObj.put(attr, jsonValue);
                    }
                }else{
                    jsonObj.put(attr, jsonValue);
                }
            }
        }

        return jsonObj;
    }

    private static final String JSON_ARRAY = "json-array-attributes";

    /**
     * Get the set of identifiers which should be treated as json arrays.
     *
     * @param id
     * @return
     */
    private static Set<String> getJsonArrayAttributes(Identifier id)
    {
        final Set<String> attrs = new HashSet<String>();

        final Iterator<Wme> wmes = id.getWmes();
        while (wmes.hasNext())
        {
            final Wme wme = wmes.next();

            final StringSymbol attr = wme.getAttribute().asString();
            final StringSymbol value = wme.getValue().asString();

            if(attr != null && attr.getValue().equals(JSON_ARRAY))
            {
                if(value != null)
                {
                    attrs.add(value.getValue());
                }
            }
        }

        return attrs;
    }
}
