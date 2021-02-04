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
package com.kumuluz.ee.graphql.utils;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.graphql.classes.*;
import com.kumuluz.ee.rest.beans.QueryFilter;
import com.kumuluz.ee.rest.beans.QueryOrder;
import com.kumuluz.ee.rest.beans.QueryParameters;
import com.kumuluz.ee.rest.enums.FilterOperation;
import com.kumuluz.ee.rest.utils.JPAUtils;
import com.kumuluz.ee.rest.utils.StreamUtils;
import graphql.GraphQLException;
import graphql.schema.DataFetchingEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;

/**
 * GraphQLUtils class - helper class for pagination, sorting and filtering
 *
 * @author Domen Kajdic
 * @author Urban Malc
 * @since 1.0.0
 */
public class GraphQLUtils<T> {

    /**
     * @deprecated Use {@link StreamUtils#queryEntities(Collection, QueryParameters)} from kumuluzee-rest
     */
    @Deprecated
    public static <T> PaginationWrapper<T> process(List<T> list, Pagination p) {
        return process(list, p, null, null);
    }

    /**
     * @deprecated Use {@link StreamUtils#queryEntities(Collection, QueryParameters)} from kumuluzee-rest
     */
    @Deprecated
    public static <T> PaginationWrapper<T> process(List<T> list, Pagination p, Sort s) {
        return process(list, p, s, null);
    }

    /**
     * @deprecated Use {@link StreamUtils#queryEntities(Collection, QueryParameters)} from kumuluzee-rest
     */
    @Deprecated
    public static <T> PaginationWrapper<T> process(List<T> list, Pagination p, Filter f) {
        return process(list, p, null, f);
    }

    private static Pagination getDefaultPagination() {
        ConfigurationUtil  configurationUtil = ConfigurationUtil.getInstance();
        return new Pagination(configurationUtil.getInteger("kumuluzee.graphql.defaults.limit").orElse(20), configurationUtil.getInteger("kumuluzee.graphql.defaults.offset").orElse(0));
    }

    /**
     * @deprecated Use {@link StreamUtils#queryEntities(Collection, QueryParameters)}
     */
    @Deprecated
    public static <T> PaginationWrapper<T> process(List<T> l, Pagination p, Sort s, Filter f) {

        QueryParameters queryParameters = queryParameters(p, s, f);
        List<T> result = StreamUtils.queryEntities(l, queryParameters);
        return wrapList(result, p, StreamUtils.queryEntitiesCount(l, queryParameters).intValue());
    }

    /**
     * @deprecated User {@link StreamUtils#queryEntities(Collection, QueryParameters)}
     */
    @Deprecated
    public static <T> List<T> processWithoutPagination(List<T> l, Sort s, Filter f) {
        QueryParameters queryParameters = queryParameters(null, s, f, false);
        return StreamUtils.queryEntities(l, queryParameters);
    }

    /**
     * @deprecated User {@link StreamUtils#queryEntities(Collection, QueryParameters)}
     */
    @Deprecated
    public static <T> List<T> processWithoutPagination(List<T> l, Filter f) {
        return processWithoutPagination(l, null, f);
    }

    /**
     * @deprecated User {@link StreamUtils#queryEntities(Collection, QueryParameters)}
     */
    @Deprecated
    public static <T> List<T> processWithoutPagination(List<T> l, Sort s) {
        return processWithoutPagination(l, s, null);
    }

    private static List<String> getStringList(String stringArray, boolean ignoreCase) {
        if(stringArray.charAt(0) == '[' && stringArray.charAt(stringArray.length()-1) == ']') {
            stringArray = stringArray.substring(1, stringArray.length()-1);
        } else {
            throw new GraphQLException("Value field must contain array ([item1,item2,...]).");
        }
        if(ignoreCase) {
            return Arrays.asList(stringArray.toLowerCase().split(","));
        } else {
            return Arrays.asList(stringArray.split(","));
        }
    }

    public static QueryParameters queryParameters(Pagination p, Sort s, Filter f) {
        return queryParameters(p,s,f, true);
    }

    public static QueryParameters queryParameters(Pagination p, Sort s, Filter f, boolean forcePagination) {
        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();
        QueryParameters qs = new QueryParameters();
        if(p == null && forcePagination) {
            p = getDefaultPagination();
        }

        if(p != null) {
            Integer offset = p.getOffset() == null ? configurationUtil.getInteger("kumuluzee.graphql.defaults.offset").orElse(0) : p.getOffset();
            Integer limit = p.getLimit() == null ? configurationUtil.getInteger("kumuluzee.graphql.defaults.limit").orElse(20) : p.getLimit();
            qs.setOffset(offset);
            qs.setLimit(limit);
        }
        if(s != null) {
            List<QueryOrder> queryOrderList = new ArrayList<>();
            for(SortField sortField: s.getFields()) {
                QueryOrder queryOrder = new QueryOrder();
                queryOrder.setField(sortField.getField());
                queryOrder.setOrder(sortField.getOrder());
                queryOrderList.add(queryOrder);
            }
            qs.setOrder(queryOrderList);
        }
        if(f != null) {
            List<QueryFilter> queryFilterList = new ArrayList<>();
            for(FilterField filterField: f.getFields()) {
                QueryFilter queryFilter;
                if(filterField.getType() == FilterType.DATE) {
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                    TemporalAccessor accessor = timeFormatter.parse(filterField.getValue());
                    queryFilter = new QueryFilter(filterField.getField(), filterField.getOp(), Date.from(Instant.from(accessor)));
                } else if(filterField.getOp() == FilterOperation.IN || filterField.getOp() == FilterOperation.NIN) {
                    queryFilter = new QueryFilter(filterField.getField(), filterField.getOp(), getStringList(filterField.getValue(), false));
                } else if(filterField.getOp() == FilterOperation.INIC ||filterField.getOp() == FilterOperation.NINIC) {
                    queryFilter = new QueryFilter(filterField.getField(), filterField.getOp(), getStringList(filterField.getValue(), true));
                } else {
                    queryFilter = new QueryFilter(filterField.getField(), filterField.getOp(), filterField.getValue());
                }
                queryFilterList.add(queryFilter);
            }
            qs.setFilters(queryFilterList);
        }
        return qs;
    }

    public static <T> PaginationWrapper<T> wrapList(List<T> list, Pagination pagination, Integer size) {
        if(pagination == null) {
            pagination = getDefaultPagination();
        }
        PaginationOutput paginationOutput = new PaginationOutput(pagination, size);
        return new PaginationWrapper<T>(paginationOutput, list);
    }

    public static <T> PaginationWrapper<T> wrapList(List<T> list, Pagination pagination) {
        return wrapList(list, pagination, null);
    }

    public static <T> PaginationWrapper<T> process(EntityManager em, Class<T> tClass, ResolutionEnvironment resolutionEnvironment, Pagination pagination, Sort sort) {
        return process(em, tClass, resolutionEnvironment, pagination, sort,null);
    }

    public static <T> PaginationWrapper<T> process(EntityManager em, Class<T> tClass, ResolutionEnvironment resolutionEnvironment, Pagination pagination, Filter filter) {
        return process(em, tClass, resolutionEnvironment, pagination, null, filter);
    }

    public static <T> PaginationWrapper<T> process(EntityManager em, Class<T> tClass, ResolutionEnvironment resolutionEnvironment, Pagination pagination) {
        return process(em, tClass,resolutionEnvironment, pagination, null,null);
    }

    public static <T> PaginationWrapper<T> process(EntityManager em, Class<T> tClass, Pagination pagination, Sort sort) {
        return process(em, tClass, null, pagination, sort,null);
    }

    public static <T> PaginationWrapper<T> process(EntityManager em, Class<T> tClass, Pagination pagination, Filter filter) {
        return process(em, tClass, null, pagination, null, filter);
    }

    public static <T> PaginationWrapper<T> process(EntityManager em, Class<T> tClass, Pagination pagination) {
        return process(em, tClass,null, pagination, null,null);
    }

    public static <T> PaginationWrapper<T> process(EntityManager em, Class<T> tClass, Pagination pagination, Sort sort, Filter filter) {
        return process(em, tClass, null, pagination, sort, filter);
    }

    public static <T> PaginationWrapper<T> process(EntityManager em, Class<T> tClass, ResolutionEnvironment resolutionEnvironment, Pagination pagination, Sort sort, Filter filter) {
        QueryParameters queryParameters = queryParameters(pagination, sort, filter, true);
        if(resolutionEnvironment != null) {
            queryParameters.setFields(getFieldsFromResolutionEnvironment(resolutionEnvironment));
        }
        List<T> studentList = JPAUtils.queryEntities(em, tClass, queryParameters);
        Long size = JPAUtils.queryEntitiesCount(em, tClass, queryParameters);
        return GraphQLUtils.wrapList(studentList, pagination, size.intValue());
    }

    public static<T> List<T> processWithoutPagination(EntityManager em, Class<T> tClass) {
        return processWithoutPagination(em, tClass, null, null, null);
    }

    public static<T> List<T> processWithoutPagination(EntityManager em, Class<T> tClass, ResolutionEnvironment resolutionEnvironment) {
        return processWithoutPagination(em, tClass, resolutionEnvironment, null, null);
    }

    public static<T> List<T> processWithoutPagination(EntityManager em, Class<T> tClass, ResolutionEnvironment resolutionEnvironment, Sort sort) {
        return processWithoutPagination(em, tClass, resolutionEnvironment, sort, null);
    }

    public static<T> List<T> processWithoutPagination(EntityManager em, Class<T> tClass, ResolutionEnvironment resolutionEnvironment, Filter filter) {
        return processWithoutPagination(em, tClass, resolutionEnvironment, null, filter);
    }

    public static<T> List<T> processWithoutPagination(EntityManager em, Class<T> tClass, Sort sort) {
        return processWithoutPagination(em, tClass, null, sort, null);
    }

    public static<T> List<T> processWithoutPagination(EntityManager em, Class<T> tClass, Filter filter) {
        return processWithoutPagination(em, tClass, null, null, filter);
    }

    public static<T> List<T> processWithoutPagination(EntityManager em, Class<T> tClass, Sort sort, Filter filter) {
        return processWithoutPagination(em, tClass, null, sort, filter);
    }

    public static<T> List<T> processWithoutPagination(EntityManager em, Class<T> tClass, ResolutionEnvironment resolutionEnvironment, Sort sort, Filter filter) {
        QueryParameters queryParameters = queryParameters(null, sort, filter, false);
        if(resolutionEnvironment != null) {
            queryParameters.setFields(getFieldsFromResolutionEnvironment(resolutionEnvironment));
        }
        return JPAUtils.queryEntities(em, tClass, queryParameters);
    }

    private static List<String> getFieldsFromResolutionEnvironment(ResolutionEnvironment resolutionEnvironment) {
        List<String> fields = new ArrayList<>();

        DataFetchingEnvironment dataFetchingEnvironment = resolutionEnvironment.dataFetchingEnvironment;
        Set<String> graphqlFields = dataFetchingEnvironment.getSelectionSet().get().keySet();

        if(graphqlFields.contains("result")) {
            for(String s: graphqlFields) {
                String[] split = s.split("/", 2);
                if(split[0].equals("result") && split.length >= 2) {
                    fields.add(String.join(".", split[1].split("/")));
                }
            }
        } else {
            for(String s: graphqlFields) {
                String[] split = s.split("/");
                if(split.length >= 1) {
                    fields.add(String.join(".", split));
                }
            }
        }

        fields.removeIf(f -> fields.stream().anyMatch(ff -> !ff.equals(f) && ff.startsWith(f)));

        return fields;
    }
}