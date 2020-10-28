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
package com.kumuluz.ee.graphql.mp.config;

import com.kumuluz.ee.configuration.ConfigurationSource;
import com.kumuluz.ee.configuration.utils.ConfigurationDispatcher;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import io.smallrye.graphql.cdi.config.ConfigKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maps MicroProfile and SmallRye configuration key to KumuluzEE namespace.
 *
 * @author Urban Malc
 * @since 1.2.0
 */
public class KumuluzConfigMapper implements ConfigurationSource {

    private static final Map<String, String> CONFIG_MAP = new HashMap<>();
    private static final Map<String, String> CONFIG_MAP_LIST = new HashMap<>();

    static {
        CONFIG_MAP.put(ConfigKey.DEFAULT_ERROR_MESSAGE, "kumuluzee.graphql.exceptions.default-error-message");
        CONFIG_MAP.put(ConfigKey.SCHEMA_INCLUDE_SCALARS, "kumuluzee.graphql.schema.include-scalars");
        CONFIG_MAP.put(ConfigKey.SCHEMA_INCLUDE_DEFINITION, "kumuluzee.graphql.schema.include-schema-definition");
        CONFIG_MAP.put(ConfigKey.SCHEMA_INCLUDE_DIRECTIVES, "kumuluzee.graphql.schema.include-directives");
        CONFIG_MAP.put(ConfigKey.SCHEMA_INCLUDE_INTROSPECTION_TYPES, "kumuluzee.graphql.schema.include-introspection-types");
        CONFIG_MAP.put(ConfigKey.ENABLE_METRICS, "kumuluzee.graphql.metrics.enabled");

        CONFIG_MAP_LIST.put("mp.graphql.hideErrorMessage", "kumuluzee.graphql.exceptions.hide-error-message");
        CONFIG_MAP_LIST.put("mp.graphql.showErrorMessage", "kumuluzee.graphql.exceptions.show-error-message");
    }

    private ConfigurationUtil configurationUtil;

    /**
     * Low ordinal in order for MP prefix to take precedence.
     */
    @Override
    public Integer getOrdinal() {
        return 10;
    }

    @Override
    public void init(ConfigurationDispatcher configurationDispatcher) {
        configurationUtil = ConfigurationUtil.getInstance();
    }

    @Override
    public Optional<String> get(String s) {

        String mappedKey = CONFIG_MAP.get(s);

        if (mappedKey != null) {
            return configurationUtil.get(mappedKey);
        }

        mappedKey = CONFIG_MAP_LIST.get(s);

        if (mappedKey != null) {
            return configurationUtil.getList(mappedKey).map(ls -> String.join(",", ls));
        }

        return Optional.empty();
    }

    @Override
    public Optional<Boolean> getBoolean(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getInteger(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> getLong(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<Double> getDouble(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<Float> getFloat(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getListSize(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> getMapKeys(String s) {
        return Optional.empty();
    }

    @Override
    public void watch(String s) {

    }

    @Override
    public void set(String s, String s1) {

    }

    @Override
    public void set(String s, Boolean aBoolean) {

    }

    @Override
    public void set(String s, Integer integer) {

    }

    @Override
    public void set(String s, Double aDouble) {

    }

    @Override
    public void set(String s, Float aFloat) {

    }
}
