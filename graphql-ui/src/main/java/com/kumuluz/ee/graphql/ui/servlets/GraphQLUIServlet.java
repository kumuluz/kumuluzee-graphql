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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;

/**
 * GraphQLUIServlet class - HttpServlet, that serves graphiql.html to user
 *
 * @author Domen Kajdic
 * @since 1.0.0
 */
public class GraphQLUIServlet extends HttpServlet {
    private String path = null;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter w = resp.getWriter();
        if(path == null) {
            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();
            path = configurationUtil.get("kumuluzee.graphql.mapping").orElse("/graphql");
            try {
                URI u = new URI(path);
                if(u.isAbsolute()) {
                    w.println("URL must be relative. Extension not initialized.");
                    return;
                }
            } catch(Exception E) {
                w.println("Malformed url: " + path + ". Extension not initialized.");
                return;
            }
            if(path.charAt(0) != '/') {
                path = '/' + path;
            }
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/html/graphiql.html")))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("$PATH")) {
                    line = line.replace("$PATH", path);
                }
                w.println(line);
            }
        }
    }
}
