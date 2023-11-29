/*
 *  Copyright (c) 2014-2018 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.graphql.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Taken from https://github.com/graphql-java/graphql-java-http-example/blob/master/src/main/java/com/graphql/example/http/utill/QueryParameters.java
 * Edited by Domen Kajdic
 *
 * @author Domen Kajdic
 * @since 1.0.0
 */
public class QueryParameters {

    private String query;
    private String operationName;
    private Map<String, Object> variables = Collections.emptyMap();

    public String getQuery() {
        return query;
    }

    public String getOperationName() {
        return operationName;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public static QueryParameters from(HttpServletRequest request) throws IOException {
        QueryParameters parameters = new QueryParameters();
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            if(request.getParameter("query") != null) {
                parameters.query = request.getParameter("query");
                parameters.operationName = request.getParameter("operationName");
                parameters.variables = getVariables(request.getParameter("variables"));
            } else if(request.getHeader("Content-Type").equals("application/graphql")) {
                parameters.query = request.getReader().lines().collect(Collectors.joining());
            } else {
                Map<String, Object> json = readJSON(request);
                parameters.query = (String) json.get("query");
                parameters.operationName = (String) json.get("operationName");
                parameters.variables = getVariables(json.get("variables"));
            }
        } else {
            parameters.query = request.getParameter("query");
            parameters.operationName = request.getParameter("operationName");
            parameters.variables = getVariables(request.getParameter("variables"));
        }
        return parameters;
    }

    private static Map<String, Object> getVariables(Object variables) {
        if (variables instanceof Map) {
            Map<?, ?> inputVars = (Map<?, ?>) variables;
            Map<String, Object> vars = new HashMap<>();
            inputVars.forEach((k, v) -> vars.put(String.valueOf(k), v));
            return vars;
        }
        try {
            Map<String, Object> vars = new ObjectMapper().readValue(String.valueOf(variables),
                    new TypeReference<Map<String, Object>>() {});
            return (vars != null) ? vars : Collections.emptyMap();
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    private static Map<String, Object> readJSON(HttpServletRequest request) {
        String s = readPostBody(request);
        try {
            Map<String, Object> json = new ObjectMapper().readValue(s, new TypeReference<Map<String, Object>>() {});
            return (json != null) ? json : Collections.emptyMap();
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    private static String readPostBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
