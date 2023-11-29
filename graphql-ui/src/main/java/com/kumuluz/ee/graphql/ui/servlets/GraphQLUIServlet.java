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

package com.kumuluz.ee.graphql.ui.servlets;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * GraphQLUIServlet class - HttpServlet, that serves graphiql.html to user
 *
 * @author Domen Kajdic
 * @since 1.0.0
 */
public class GraphQLUIServlet extends HttpServlet {

    private String graphQlPath = null;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        if (this.graphQlPath == null) {
            String contextPath = configurationUtil.get("kumuluzee.server.context-path").orElse("");
            String path = configurationUtil.get("kumuluzee.graphql.mapping").orElse("/graphql");

            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }

            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            path = contextPath + path;

            try {
                URI u = new URI(path);

                if (u.isAbsolute()) {
                    resp.getWriter().println("URL must be relative. Extension not initialized.");
                    return;
                }
            } catch (Exception E) {
                resp.getWriter().println("Malformed url: " + path + ". Extension not initialized.");
                return;
            }

            if (path.charAt(0) != '/') {
                path = '/' + path;
            }

            graphQlPath = path;
        }

        if (req.getPathInfo() == null) {
            // no trailing slash, redirect to trailing slash in order to fix relative requests
            resp.sendRedirect(req.getContextPath() + req.getServletPath() + "/");
            return;
        }

        if ("/main.js".equals(req.getPathInfo())) {
            resp.setContentType("application/javascript");
            // inject _kumuluzee_graphql_path variable into js
            sendFile(resp, "main.js", "_kumuluzee_graphql_path = \"" + graphQlPath + "\";\n");
        } else {
            sendFile(resp, "index.html", null);
        }
    }

    private void sendFile(HttpServletResponse resp, String file, String prepend) throws IOException {
        InputStream in = this.getClass().getResourceAsStream("/html/" + file);
        OutputStream out = resp.getOutputStream();

        if (prepend != null) {
            out.write(prepend.getBytes());
        }

        byte[] buf = new byte[10000];
        int length;
        while ((length = in.read(buf)) > 0) {
            out.write(buf, 0, length);
        }

        in.close();
        out.close();
    }
}
