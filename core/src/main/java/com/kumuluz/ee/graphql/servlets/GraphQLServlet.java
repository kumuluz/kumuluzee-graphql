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

package com.kumuluz.ee.graphql.servlets;

import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.runtime.EeRuntime;
import com.kumuluz.ee.common.runtime.EeRuntimeComponent;
import com.kumuluz.ee.graphql.utils.JsonKit;
import com.kumuluz.ee.graphql.utils.QueryParameters;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;

import javax.enterprise.inject.spi.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * GraphQLServlet class - http servlet for exposing GraphQL endpoint
 *
 * @author Domen Kajdic
 * @since 1.0.0
 */
public class GraphQLServlet extends HttpServlet {
    private GraphQLSchema schema;
    private GraphQL graphQL;
    private static final Logger LOG = Logger.getLogger(GraphQLServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        QueryParameters parameters = QueryParameters.from(req);
        if (parameters.getQuery() == null) {
            resp.setStatus(400);
            return;
        }
        processQuery(parameters, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        QueryParameters parameters = QueryParameters.from(req);
        if (parameters.getQuery() == null) {
            resp.setStatus(400);
            return;
        }
        processQuery(parameters, resp);
    }

    private void processQuery(QueryParameters parameters, HttpServletResponse resp) throws IOException {
        ExecutionInput.Builder executionInput = ExecutionInput.newExecutionInput()
                .query(parameters.getQuery())
                .operationName(parameters.getOperationName())
                .variables(parameters.getVariables());
        if(schema == null) {
            schema = buildSchema();
        }

        if(graphQL == null) {
            graphQL = GraphQL
                    .newGraphQL(schema)
                    .build();
        }
        ExecutionResult executionResult = graphQL.execute(executionInput.build());
        returnAsJson(resp, executionResult);
    }

    private GraphQLSchema buildSchema()  {
        try {
            List<EeRuntimeComponent> components = EeRuntime.getInstance().getEeComponents();
            boolean CDIfound = false;
            for(EeRuntimeComponent component : components) {
                if(component.getType() == EeComponentType.CDI) {
                    CDIfound = true;
                    break;
                }
            }
            List<Class<?>> classes = getResourceClasses();
            GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator();
            for(Class c: classes) {
                if(CDIfound) {
                    //we have CDI, perform injections
                    generator.withOperationsFromSingleton(getUnmanagedInstance(c), c);
                } else {
                    //no CDI, use newInstance()
                    Object o = c.newInstance();
                    generator.withOperationsFromSingleton(o);
                }
            }
            return generator.generate();
        } catch(Exception e) {
            LOG.severe(e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object getUnmanagedInstance(Class c) {
        Unmanaged unmanaged = new Unmanaged(c);
        Unmanaged.UnmanagedInstance unmanagedInstance = unmanaged.newInstance().produce().inject().postConstruct();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            unmanagedInstance.preDestroy().dispose();
        }));
        return unmanagedInstance.get();
    }

    private void returnAsJson(HttpServletResponse response, ExecutionResult executionResult) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        JsonKit.toJson(response, executionResult.toSpecification());
    }

    public List<Class<?>> getResourceClasses() {
        List<Class<?>> resourceClasses = new ArrayList<>();

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream is = classLoader.getResourceAsStream("META-INF/kumuluzee/graphql/java.lang.Object");

        if (is != null) {
            Scanner scanner = new Scanner(is);
            while (scanner.hasNextLine()) {
                String className = scanner.nextLine();
                try {
                    Class resourceClass = Class.forName(className);
                    resourceClasses.add(resourceClass);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            scanner.close();
        }

        return resourceClasses;
    }
}
