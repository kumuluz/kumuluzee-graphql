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
package com.kumuluz.ee.graphql.mp;

import org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import java.util.Arrays;

/**
 * Skips tests, which are disabled for various reasons.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class SkipTestListener implements IInvokedMethodListener {

    private static final String[] SKIP_TESTS = {
            "testJsonDefault"
    };

    private static final String SKIP_ATTRIBUTE = "KUMULUZEE_SKIP_TEST";

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        TestData tdE = getExecutionTestData(testResult.getParameters());
        org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData tdS = getSchemaTestData(testResult.getParameters());
        if (tdE != null && Arrays.stream(SKIP_TESTS).anyMatch(s -> s.equals(tdE.getName())) ||
                tdS != null && Arrays.stream(SKIP_TESTS).anyMatch(s -> s.equals(tdS.getHeader()))) {
            testResult.setAttribute(SKIP_ATTRIBUTE, true);
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (testResult.getAttribute(SKIP_ATTRIBUTE) != null) {
            testResult.setStatus(ITestResult.SKIP);
        }
    }

    /**
     * From: https://github.com/smallrye/smallrye-graphql/blob/1.0.14/server/tck/src/test/java/io/smallrye/graphql/TestInterceptor.java
     */
    private TestData getExecutionTestData(Object[] parameters) {
        for (Object param : parameters) {
            if (param instanceof TestData) {
                return (TestData) param;
            }
        }
        return null;
    }

    private org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData getSchemaTestData(Object[] parameters) {
        for (Object param : parameters) {
            if (param instanceof org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData) {
                return (org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData) param;
            }
        }
        return null;
    }
}

