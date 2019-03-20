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

package com.kumuluz.ee.graphql.ui;

import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.graphql.ui.servlets.GraphQLUIServlet;
import com.kumuluz.ee.jetty.JettyServletServer;

import java.util.logging.Logger;

/**
 * GraphQLUIExtension class - extension class for hosting GraphiQL
 *
 * @author Domen Kajdic
 * @since 1.0.0
 */
@EeExtensionDef(name = "GraphQLUI", group = "GRAPHQL_UI")
public class GraphQLUIExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(GraphQLUIExtension.class.getName());

    @Override
    public void load() {
    }

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {
        if (kumuluzServerWrapper.getServer() instanceof JettyServletServer) {
            LOG.info("Initializing GraphQL UI extension.");
            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

            try {
                Class.forName("com.kumuluz.ee.graphql.GraphQLExtension");
            } catch (ClassNotFoundException e) {
                LOG.severe("Unable to find GraphQL extension, GraphQL UI will not be initialized: " + e.getMessage());
                return;
            }

            boolean production = configurationUtil.get("kumuluzee.env.name").orElse("dev").equals("production");
            if(configurationUtil.getBoolean("kumuluzee.graphql.ui.enabled").orElse(!production)) {
                String mapping = configurationUtil.get("kumuluzee.graphql.ui.mapping").orElse("/graphiql");
                if(mapping.charAt(0) != '/') {
                    mapping = '/' + mapping;
                }
                JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();
                server.registerServlet(GraphQLUIServlet.class, mapping);
                LOG.info("GraphQL UI extension initialized.");
            } else {
                LOG.info("GraphQL UI disabled. You can enable it explicitly by setting field kumuluzee.graphql.ui.enabled to true.");
            }
        }
    }
}
