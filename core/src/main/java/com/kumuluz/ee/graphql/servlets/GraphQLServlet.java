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
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.graphql.GraphQLApplication;
import com.kumuluz.ee.graphql.utils.JsonKit;
import com.kumuluz.ee.graphql.utils.QueryParameters;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ExecutionIdProvider;
import graphql.execution.ExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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
    private HashMap<String, Object> contexts = new HashMap<>();
    private ChainedInstrumentation chainedInstrumentation = null;
    private ExecutionStrategy queryExecutionStrategy = null;
    private ExecutionStrategy mutationExecutionStrategy = null;
    private ExecutionStrategy subscriptionExecutionStrategy = null;
    private PreparsedDocumentProvider preparsedDocumentProvider = null;
    private ExecutionIdProvider executionIdProvider = null;
    private boolean perRequest = false;
    private static final Logger LOG = Logger.getLogger(GraphQLServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        QueryParameters parameters = QueryParameters.from(req);
        if (parameters.getQuery() == null) {
            resp.setStatus(400);
            return;
        }
        processQuery(parameters, resp);
    }

    private void processQuery(QueryParameters parameters, HttpServletResponse resp) throws IOException {
        if (schema == null) {
            List<GraphQLApplication> applications = new ArrayList<>();
            ServiceLoader.load(GraphQLApplication.class).forEach(applications::add);
            Class<?> applicationClass;
            if (applications.size() == 1) {
                applicationClass = applications.get(0).getClass();
            } else {
                applicationClass = GraphQLApplication.class;
            }
            try {
                GraphQLApplication app = (GraphQLApplication) applicationClass.newInstance();
                contexts = app.setContexts();
                chainedInstrumentation = new ChainedInstrumentation(app.setInstrumentations());
                queryExecutionStrategy = app.setQueryExecutionStrategy();
                mutationExecutionStrategy = app.setMutationExecutionStrategy();
                subscriptionExecutionStrategy = app.setSubscriptionExecutionStrategy();
                preparsedDocumentProvider = app.setPreparsedDocumentProvider();
                executionIdProvider = app.setExecutionIdProvider();
                perRequest = app.setPerRequestBuilder();
            } catch (Exception e) {
                LOG.severe(e.getMessage());
            }
            schema = buildSchema();
        }

        if (graphQL == null) {
            GraphQL.Builder builder = GraphQL
                    .newGraphQL(schema)
                    .instrumentation(chainedInstrumentation);
            if (queryExecutionStrategy != null) {
                builder.queryExecutionStrategy(queryExecutionStrategy);
            }
            if (mutationExecutionStrategy != null) {
                builder.mutationExecutionStrategy(mutationExecutionStrategy);
            }
            if (subscriptionExecutionStrategy != null) {
                builder.subscriptionExecutionStrategy(subscriptionExecutionStrategy);
            }
            if (preparsedDocumentProvider != null) {
                builder.preparsedDocumentProvider(preparsedDocumentProvider);
            }
            if (executionIdProvider != null) {
                builder.executionIdProvider(executionIdProvider);
            }
            graphQL = builder.build();
        }

        ExecutionInput.Builder executionInput = ExecutionInput.newExecutionInput()
                .query(parameters.getQuery())
                .operationName(parameters.getOperationName())
                .variables(parameters.getVariables())
                .context(contexts);

        ExecutionResult executionResult = graphQL.execute(executionInput.build());
        returnAsJson(resp, executionResult);
        if (perRequest) {
            graphQL = null;
        }
    }

    private GraphQLSchema buildSchema() {
        final List<String> basePackages = new ArrayList<>(
                Collections.singletonList("com.kumuluz.ee.graphql.classes")
        );

        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        try {
            List<EeRuntimeComponent> components = EeRuntime.getInstance().getEeComponents();
            boolean CDIfound = false;

            for (EeRuntimeComponent component : components) {
                if (component.getType() == EeComponentType.CDI) {
                    CDIfound = true;
                    break;
                }
            }

            List<Class<?>> classes = getResourceClasses();

            GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator();

            configurationUtil.getList("kumuluzee.graphql.schema.base-packages")
                    .ifPresent(basePackages::addAll);

            generator.withBasePackages(basePackages.toArray(new String [0]));

            for (Class<?> c : classes) {
                if (CDIfound) {
                    //we have CDI, perform injections
                    try {
                        generator.withOperationsFromSingleton(CDI.current().select(c).get(), c);
                    } catch (Exception e) {
                        generator.withOperationsFromSingleton(c.getDeclaredConstructor().newInstance(), c);
                    }
                } else {
                    //no CDI, use newInstance()
                    generator.withOperationsFromSingleton(c.getDeclaredConstructor().newInstance(), c);
                }
            }

            return generator.generate();
        } catch (Exception e) {
            LOG.severe(e.getMessage());
        }
        return null;
    }

    private void returnAsJson(HttpServletResponse response, ExecutionResult executionResult) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        JsonKit.toJson(response, executionResult.toSpecification());
    }

    private List<Class<?>> getResourceClasses() {
        List<Class<?>> resourceClasses = new ArrayList<>();

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream is = classLoader.getResourceAsStream("META-INF/kumuluzee/graphql/java.lang.Object");

        if (is != null) {
            Scanner scanner = new Scanner(is);
            while (scanner.hasNextLine()) {
                String className = scanner.nextLine();
                try {
                    Class<?> resourceClass = Class.forName(className);
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
