/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
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

import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.*;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.jetty.JettyServletServer;
import io.smallrye.graphql.servlet.ExecutionServlet;
import io.smallrye.graphql.servlet.SchemaServlet;

import java.net.URI;
import java.util.logging.Logger;

/**
 * KumuluzEE MicroProfile GraphQL extension, implemented by SmallRye.
 *
 * @author Urban Malc
 * @since 1.2.0
 */
@EeExtensionDef(name = "MicroProfileGraphQL", group = EeExtensionGroup.GRAPHQL)
@EeComponentDependencies({
        @EeComponentDependency(EeComponentType.SERVLET),
        @EeComponentDependency(EeComponentType.JSON_B),
        @EeComponentDependency(EeComponentType.CDI),
})
public class GraphQLExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(GraphQLExtension.class.getName());

    @Override
    public boolean isEnabled() {
        return ConfigurationUtil
                .getInstance()
                .getBoolean("kumuluzee.graphql.enabled")
                .orElse(true);
    }

    @Override
    public void load() {
        try {
            Class.forName("com.kumuluz.ee.config.microprofile.MicroprofileConfigExtension");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not find KumuluzEE Config MP dependency, required by " +
                    "KumuluzEE GraphQL extension. " +
                    "Please add it as a dependency: https://github.com/kumuluz/kumuluzee-config-mp");
        }
    }

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {
        if (kumuluzServerWrapper.getServer() instanceof JettyServletServer) {
            LOG.info("Initializing GraphQL MP extension.");
            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

            String path = configurationUtil.get("kumuluzee.graphql.mapping").orElse("graphql");
            try {
                URI u = new URI(path);

                if(u.isAbsolute()) {
                    LOG.severe("URL must be relative. Extension not initialized.");
                    return;
                }
            } catch(Exception E) {
                LOG.severe("Malformed url: " + path + ". Extension not initialized.");
                return;
            }

            // strip "/"
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();
            server.registerServlet(SchemaServlet.class, "/" + path + "/schema.graphql");
            server.registerServlet(ExecutionServlet.class, "/" + path + "/*");

            LOG.info("GraphQL MP registered on /" + path + " (servlet context is implied).");
            LOG.info("GraphQL MP extension initialized.");
        }
    }
}
