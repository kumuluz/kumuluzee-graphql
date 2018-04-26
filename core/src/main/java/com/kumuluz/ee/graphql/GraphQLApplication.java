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

package com.kumuluz.ee.graphql;

import graphql.execution.ExecutionIdProvider;
import graphql.execution.ExecutionStrategy;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.preparsed.PreparsedDocumentProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * GraphQLApplication class - add settings to GraphQL endpoint at runtime
 *
 * @author Domen Kajdic
 * @since 1.0.0
 */
public class GraphQLApplication {
    public HashMap<String, Object> setContexts() {
        return new HashMap<>();
    }

    public List<Instrumentation> setInstrumentations() {
        return new ArrayList<>();
    }

    public ExecutionStrategy setQueryExecutionStrategy() {
        return null;
    }

    public ExecutionStrategy setMutationExecutionStrategy() {
        return null;
    }

    public ExecutionStrategy setSubscriptionExecutionStrategy() {
        return null;
    }

    public PreparsedDocumentProvider setPreparsedDocumentProvider() {
        return null;
    }

    public ExecutionIdProvider setExecutionIdProvider() {
        return null;
    }

    public boolean setPerRequestBuilder() {
        return false;
    }
}
