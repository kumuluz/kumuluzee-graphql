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

/**
 * PaginationOutput class - pagination output when requesting pagination data
 *
 * @author Domen Kajdic
 * @since 1.0.0
 */
public class PaginationOutput extends Pagination {
    private Integer total;

    public PaginationOutput(Pagination p, Integer total) {
        super(p.getLimit(), p.getOffset());
        this.total = total;
    }

    public Integer getTotal() {
        return total;
    }
}
