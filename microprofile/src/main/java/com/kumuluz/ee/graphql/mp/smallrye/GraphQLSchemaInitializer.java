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
package com.kumuluz.ee.graphql.mp.smallrye;

import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.utils.ResourceUtils;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.graphql.mp.utils.JarUtils;
import graphql.schema.GraphQLSchema;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import io.smallrye.graphql.cdi.producer.GraphQLProducer;
import io.smallrye.graphql.schema.SchemaBuilder;
import io.smallrye.graphql.schema.model.Schema;
import io.smallrye.graphql.servlet.SchemaServlet;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Scans classpath for annotations and initializes schema.
 *
 * @author Urban Malc
 * @since 1.2.0
 */
@ApplicationScoped
public class GraphQLSchemaInitializer {

    private static final Logger LOG = Logger.getLogger(GraphQLSchemaInitializer.class.getSimpleName());

    @Inject
    private GraphQLProducer graphQLProducer;

    public void initialize(@Observes @Initialized(ApplicationScoped.class) Object init) {

        if (!(init instanceof ServletContext)) {
            LOG.warning("Could not initialize GraphQL schema, not running in servlet context.");
            return;
        }

        ServletContext servletContext = (ServletContext) init;

        IndexView index = getIndex();

        Schema schema = SchemaBuilder.build(index); // Get the smallrye schema
        GraphQLSchema graphQLSchema = graphQLProducer.initialize(schema);

        servletContext.setAttribute(SchemaServlet.SCHEMA_PROP, graphQLSchema);
    }

    private IndexView getIndex() {

        ClassGraph classGraph = new ClassGraph().enableClassInfo();

        if (ConfigurationUtil.getInstance().getBoolean("kumuluzee.graphql.scanning.debug").orElse(false)) {
            classGraph = classGraph.verbose();
        }

        if (ConfigurationUtil.getInstance().getBoolean("kumuluzee.graphql.scanning.optimize").orElse(true)) {
            List<String> scanJars = new LinkedList<>(); // which jars should ClassGraph scan

            // if in jar add main jar name
            if (ResourceUtils.isRunningInJar()) {
                try {

                    Class.forName("com.kumuluz.ee.loader.EeClassLoader");
                    scanJars.add(JarUtils.getMainJarName());

                } catch (ClassNotFoundException e) {
                    // this should not fail since we check if we are running in jar beforehand
                    // if you get this warning you are probably doing something weird with packaging
                    LOG.warning("Could not load EeClassLoader, OpenAPI specification may not work as expected. " +
                            "Are you running in UberJAR created by KumuluzEE Maven plugin?");
                }
            }

            // add jars from kumuluzee.dev.scan-libraries configuration
            List<String> scanLibraries = EeConfig.getInstance().getDev().getScanLibraries();
            if (scanLibraries != null) {
                scanJars.addAll(scanLibraries);
            }

            if (scanJars.isEmpty()) {
                // running exploded with no scan-libraries defined in config
                classGraph.disableJarScanning();
            } else {
                // running in jar or scan-libraries defined in config
                for (String scanJar : scanJars) {
                    // scan-libraries allows two formats:
                    // - artifact-1.0.0-SNAPSHOT.jar
                    // - artifact
                    if (scanJar.endsWith(".jar")) {
                        classGraph.whitelistJars(scanJar);
                    } else {
                        classGraph.whitelistJars(scanJar + "-*.jar");
                    }
                }
            }
        }
        ScanResult scanResult = classGraph.scan();

        ClassInfoList classInfoList = scanResult.getAllClasses();
        Indexer indexer = new Indexer();
        ClassLoader classLoader = getClass().getClassLoader();

        for (ClassInfo classInfo : classInfoList) {
            try {
                indexer.index(classLoader.getResourceAsStream(classInfo.getName().replaceAll("\\.", "/") + ".class"));
            } catch (IOException e) {
                LOG.warning("Skipped scanning class: " + classInfo.getName());
            }
        }
        scanResult.close();

        return indexer.complete();
    }
}
