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

import com.kumuluz.ee.rest.enums.FilterOperation;
import io.leangen.graphql.annotations.GraphQLNonNull;

/**
 * FilterField class
 *
 * @author Domen Kajdic
 * @since 1.0.0
 */
public class FilterField {
    private FilterOperation op;
    private String field;
    private String value;
    private FilterType type;

    public FilterField() {
    }

    public FilterOperation getOp() {
        return op;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }

    public FilterType getType() {
        return type;
    }

    public void setOp(@GraphQLNonNull FilterOperation op) {
        this.op = op;
    }

    public void setField(@GraphQLNonNull String field) {
        this.field = field;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setType(FilterType type) {
        this.type = type;
    }
}
