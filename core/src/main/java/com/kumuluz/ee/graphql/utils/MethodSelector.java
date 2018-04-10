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

import graphql.GraphQLException;

import java.lang.reflect.Method;

/**
 * MethodSelector class - used for location getter functions for filtering/sorting
 *
 * @author Domen Kajdic
 * @since 1.0.0
 */
public class MethodSelector {
    private Method method;
    private String methodName;
    private Object o1;
    private Object o2;

    MethodSelector(String fieldName, Object first) {
        this(fieldName, first,null);
    }

    MethodSelector(String fieldName, Object first, Object second) {
        o1 = first;
        o2 = second;
        String[] split = fieldName.split("\\.");
        if(o1 == null) {
            throw new GraphQLException("You must provide at least one object.");
        }
        try {
            if (split.length > 1) {
                Class current = o1.getClass();
                for (int i = 0; i < split.length; i++) {
                    String s = split[i];
                    methodName = "get" + s.substring(0, 1).toUpperCase() + s.substring(1);
                    method = current.getMethod(methodName);
                    if (method.getReturnType().equals(java.util.List.class)) {
                        throw new GraphQLException("Unable to sort/filter on a list.");
                    } else {
                        if (i != split.length - 1) {
                            o1 = method.invoke(o1);
                            if(o2 != null) {
                                o2 = method.invoke(o2);
                            }
                            current = o1.getClass();
                        }
                    }
                }
            } else {
                methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                method = o1.getClass().getMethod(methodName);
                if(method.getReturnType().equals(java.util.List.class)) {
                    throw new GraphQLException("Unable to sort/filter on a list.");
                }
            }
            //should check here if method.getReturnType() is a string, int, double or float, because comparing on some other type is pointless
        } catch (Exception e) {
            throw new GraphQLException("Unable to find requested getter. " + e.getMessage());
        }
    }

    public Method getMethod() {
        return method;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object getO1() {
        return o1;
    }

    public Object getO2() {
        return o2;
    }
}


/* original code
String[] split = field.getField().split("\\.");
Method m = null;
String methodName = null;
Object o1 = t;
Object o2 = t1;
if (split.length > 1) {
    Class current = t.getClass();
    for (int i = 0; i < split.length; i++) {
        String s = split[i];
        methodName = "get" + s.substring(0, 1).toUpperCase() + s.substring(1);
        m = current.getMethod(methodName);
        if (m.getReturnType().equals(java.util.List.class)) {
            throw new GraphQLException("Unable to sort on a list.");
        } else {
            if (i != split.length - 1) {
                o1 = m.invoke(o1);
                o2 = m.invoke(o2);
                current = o1.getClass();
            }
        }
    }
} else {
    methodName = "get" + field.getField().substring(0, 1).toUpperCase() + field.getField().substring(1);
    m = t.getClass().getMethod(methodName);
    if(m.getReturnType().equals(java.util.List.class)) {
        throw new GraphQLException("Unable to sort on a list.");
    }
}
*/
