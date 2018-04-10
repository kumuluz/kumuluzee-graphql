/*
Taken from https://github.com/graphql-java/graphql-java-http-example/blob/master/src/main/java/com/graphql/example/http/utill/JsonKit.java
 */

package com.kumuluz.ee.graphql.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class JsonKit {
    static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    public static void toJson(HttpServletResponse response, Object result) throws IOException {
        GSON.toJson(result, response.getWriter());
    }

    public static Map<String, Object> toMap(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().length() == 0) {
            return Collections.emptyMap();
        }
        TypeToken<Map<String, Object>> typeToken = new TypeToken<Map<String, Object>>() {
        };
        Map<String, Object> map = GSON.fromJson(jsonStr, typeToken.getType());
        return map == null ? Collections.emptyMap() : map;
    }
}
