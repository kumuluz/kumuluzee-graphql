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
package com.kumuluz.ee.graphql.mp.utils;

import com.kumuluz.ee.loader.EeClassLoader;

/**
 * Utilities when running in UberJAR.
 *
 * @author Urban Malc
 * @since 1.2.0
 */
public class JarUtils {

    /**
     * Returns the name of the main jar when running in JAR. This needs to be in a separate class since
     * {@link EeClassLoader} is not present in all deployments (e.g. exploded).
     *
     * @return Name of the main JAR.
     */
    public static String getMainJarName() {
        // use EeClassLoader to locate main JAR since it is loaded from there
        return new java.io.File(EeClassLoader.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();
    }
}
