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

import com.kumuluz.ee.rest.beans.QueryParameters;
import com.kumuluz.ee.rest.utils.QueryStringDefaults;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for kumuluzee-rest integration.
 *
 * @author Urban Malc
 * @since 1.2.0
 */
public class GraphQLUtils {

    private GraphQLUtils() {
    }

    public static QueryParametersBuilder queryParametersBuilder() {
        return new QueryParametersBuilder();
    }

    public static class QueryParametersBuilder {

        private QueryStringDefaults qsd;
        private Long offset;
        private Long limit;
        private String order;
        private String filter;

        private QueryParametersBuilder() {
        }

        public QueryParametersBuilder withQueryStringDefaults(QueryStringDefaults qsd) {
            this.qsd = qsd;
            return this;
        }

        public QueryParametersBuilder withOffset(Integer offset) {

            if (offset == null) {
                this.offset = null;
            } else {
                this.offset = offset.longValue();
            }

            return this;
        }

        public QueryParametersBuilder withOffset(Long offset) {
            this.offset = offset;
            return this;
        }

        public QueryParametersBuilder withLimit(Integer limit) {

            if (limit == null) {
                this.limit = null;
            } else {
                this.limit = limit.longValue();
            }

            return this;
        }

        public QueryParametersBuilder withLimit(Long limit) {
            this.limit = limit;
            return this;
        }

        public QueryParametersBuilder withOrder(String order) {
            this.order = order;
            return this;
        }

        public QueryParametersBuilder withFilter(String filter) {
            this.filter = filter;
            return this;
        }

        public QueryParameters build() {

            if (qsd == null) {
                qsd = new QueryStringDefaults();
            }

            List<String> queryParts = new ArrayList<>();

            if (offset != null) {
                queryParts.add(String.format("offset=%d", offset));
            }
            if (limit != null) {
                queryParts.add(String.format("limit=%d", limit));
            }
            if (order != null) {
                queryParts.add(String.format("order=%s", order));
            }
            if (filter != null) {
                queryParts.add(String.format("filter=%s", filter));
            }

            return qsd.builder()
                    .query(String.join("&", queryParts))
                    .build();
        }
    }
}
