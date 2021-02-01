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

import com.kumuluz.ee.graphql.mp.smallrye.GraphQLSchemaInitializer;
import com.kumuluz.ee.graphql.mp.utils.JarUtils;
import org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Adds GraphQL library (this repo) to deployments.
 *
 * @author Urban Malc
 * @since 1.2.0
 */
public class GraphQLLibraryAppender extends CachedAuxilliaryArchiveAppender {

    @Override
    protected Archive<?> buildArchive() {

        return ShrinkWrap.create(JavaArchive.class, "kumuluzee-graphql.jar")
                .addClass(GraphQLExtension.class)
                .addPackages(true, JarUtils.class.getPackage())
                .addPackages(true, GraphQLSchemaInitializer.class.getPackage())
                .addAsServiceProvider(com.kumuluz.ee.common.Extension.class, GraphQLExtension.class)
                .addAsResource("META-INF/beans.xml");
    }
}
