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
package com.kumuluz.ee.graphql.classes;


import io.leangen.graphql.annotations.GraphQLNonNull;

/**
 * Pagination class - pagination input when querying with pagination
 *
 * @author Domen Kajdic
 * @since 1.0.0
 */
public class Pagination {
    private Integer limit;
    private Integer offset;

    public Pagination() {
    }

    public Pagination(Integer limit, Integer offset) {
        this.limit = limit;
        this.offset = offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public  Integer getOffset() {
        return offset;
    }

    public void setLimit(@GraphQLNonNull Integer limit) {
        this.limit = limit;
    }

    public void setOffset(@GraphQLNonNull Integer offset) {
        this.offset = offset;
    }
}
