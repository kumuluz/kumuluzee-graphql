/*
Taken from https://github.com/graphql-java/graphql-java-http-example/blob/master/src/main/java/com/graphql/example/http/utill/QueryParameters.java
Edited by Domen Kajdic
 */

package com.kumuluz.ee.graphql.utils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
            Map<?, ?> inputVars = (Map) variables;
            Map<String, Object> vars = new HashMap<>();
            inputVars.forEach((k, v) -> vars.put(String.valueOf(k), v));
            return vars;
        }
        return JsonKit.toMap(String.valueOf(variables));
    }

    private static Map<String, Object> readJSON(HttpServletRequest request) {
        String s = readPostBody(request);
        return JsonKit.toMap(s);
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
