package com.kumuluz.ee.graphql.mp;

import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeComponentDependency;
import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
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
@EeExtensionDef(name = "MicroProfileGraphQL", group = "GRAPHQL")
@EeComponentDependency(EeComponentType.SERVLET)
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
    }

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {
        if (kumuluzServerWrapper.getServer() instanceof JettyServletServer) {
            LOG.info("Initializing GraphQL extension.");
            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

            String path = configurationUtil.get("kumuluzee.graphql.mapping").orElse("/graphql");
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

            if (path.charAt(0) != '/') {
                path = '/' + path;
            }

            JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();
            server.registerServlet(SchemaServlet.class, "/graphql/schema.graphql"); // TODO use path
            server.registerServlet(ExecutionServlet.class, "/graphql/*"); // TODO use path

            LOG.info("GraphQL registered on " + path + " (servlet context is implied).");
            LOG.info("GraphQL extension initialized.");
        }
    }
}
