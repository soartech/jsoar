package org.jsoar.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jsoar.debugger.syntax.SyntaxSettings;

import java.io.File;
import java.io.IOException;

public class Prefs {
    public static boolean storeSyntax(SyntaxSettings syntax) {
        String path = System.getProperty("user.home") + "/.jsoar";

        try {
            File file = new File(path);
            if (!file.exists()) {
                file.mkdir();
            }
            file = new File(file, "syntax.json");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(file, syntax);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static SyntaxSettings loadSyntax() {
        String path = System.getProperty("user.home") + "/.jsoar";
        try {
            File file = new File(path);
            if (!file.exists())
                return null;

            file = new File(file, "syntax.json");
            if (!file.exists())
                return null;

            ObjectMapper mapper = new ObjectMapper();
            TypeReference<SyntaxSettings> highlightsType = new TypeReference<SyntaxSettings>() {
            };
            return mapper.readValue(file, highlightsType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
