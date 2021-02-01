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
import com.kumuluz.ee.rest.enums.OrderDirection;
import com.kumuluz.ee.rest.utils.JPAUtils;

import graphql.GraphQLException;
import graphql.language.*;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import io.leangen.graphql.execution.ResolutionEnvironment;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GraphQLUtils class - helper class for pagination, sorting and filtering
 *
 * @author Domen Kajdic
 * @since 1.0.0
 */
public class GraphQLUtils<T> {
    public static <T> PaginationWrapper<T> process(List<T> list, Pagination p) {
        return process(list, p, null, null);
    }

    public static <T> PaginationWrapper<T> process(List<T> list, Pagination p, Sort s) {
        return process(list, p, s, null);
    }

    public static <T> PaginationWrapper<T> process(List<T> list, Pagination p, Filter f) {
        return process(list, p, null, f);
    }

    private static Pagination getDefaultPagination() {
        ConfigurationUtil  configurationUtil = ConfigurationUtil.getInstance();
        return new Pagination(configurationUtil.getInteger("kumuluzee.graphql.defaults.limit").orElse(20), configurationUtil.getInteger("kumuluzee.graphql.defaults.offset").orElse(0));
    }

    public static <T> PaginationWrapper<T> process(List<T> l, Pagination p, Sort s, Filter f) {
        List<T> list = new ArrayList<>(l);
        list = filter(list, f);
        list = sort(list, s);
        ConfigurationUtil  configurationUtil = ConfigurationUtil.getInstance();
        if(p == null) {
            p = getDefaultPagination();
        }
        Integer offset = p.getOffset() == null ? configurationUtil.getInteger("kumuluzee.graphql.defaults.offset").orElse(0) : p.getOffset();
        Integer limit = p.getLimit() == null ? configurationUtil.getInteger("kumuluzee.graphql.defaults.limit").orElse(20) : p.getLimit();
        int count = list.size();
        PaginationOutput output = new PaginationOutput(p, count);
        if(offset > count) {
            return new PaginationWrapper<T>(output, new ArrayList<>());
        } else if(offset + limit > count) {
            return new PaginationWrapper<T>(output, list.subList(offset, count));
        }
        return new PaginationWrapper<T>(output, list.subList(offset, offset + limit));
    }

    public static <T> List<T> processWithoutPagination(List<T> l, Sort s, Filter f) {
        List<T> list = new ArrayList<>(l);
        list = filter(list, f);
        list = sort(list, s);
        return list;
    }

    public static <T> List<T> processWithoutPagination(List<T> l, Filter f) {
        List<T> list = new ArrayList<>(l);
        list = filter(list, f);
        return list;
    }

    public static <T> List<T> processWithoutPagination(List<T> l, Sort s) {
        List<T> list = new ArrayList<>(l);
        list = sort(list, s);
        return list;
    }

    private static <T> List<T> sort(List<T> list, Sort s) {
        if(s != null && list.size() > 0) {
            List<SortField> fields = s.getFields();
            if (fields.size() != 0) {
                Collections.sort(list, new Comparator<T>() {
                    @Override
                    public int compare(T t, T t1) {
                        for (SortField field : fields) {
                            try {
                                MethodSelector methodSelector = new MethodSelector(field.getField(), t, t1);
                                Method m = methodSelector.getMethod();
                                Object invoked1 = m.invoke(methodSelector.getO1());
                                Object invoked2 = m.invoke(methodSelector.getO2());
                                if(invoked1 != null && invoked2 != null) {
                                    if (m.getReturnType().toString().equals("int") || m.getReturnType() == Integer.class) {
                                        Integer i1 = (Integer)invoked1;
                                        Integer i2 = (Integer)invoked2;
                                        Integer compare = i1.compareTo(i2);
                                        if (compare != 0) {
                                            if (field.getOrder() == OrderDirection.DESC) {
                                                compare = compare * -1;
                                            }
                                            return compare;
                                        }
                                    } else if (m.getReturnType().toString().equals("double") || m.getReturnType() == Double.class) {
                                        Double i1 = (Double) invoked1;
                                        Double i2 = (Double) invoked2;
                                        Integer compare = i1.compareTo(i2);
                                        if (compare != 0) {
                                            if (field.getOrder() == OrderDirection.DESC) {
                                                compare = compare * -1;
                                            }
                                            return compare;
                                        }
                                    } else if (m.getReturnType().toString().equals("float") || m.getReturnType() == Float.class) {
                                        Float i1 = (Float) invoked1;
                                        Float i2 = (Float) invoked2;
                                        Integer compare = i1.compareTo(i2);
                                        if (compare != 0) {
                                            if (field.getOrder() == OrderDirection.DESC) {
                                                compare = compare * -1;
                                            }
                                            return compare;
                                        }
                                    } else if (m.getReturnType() == java.util.Date.class || m.getReturnType() == java.sql.Date.class) {
                                        Date d1 = (Date) invoked1;
                                        Date d2 = (Date) invoked2;
                                        Integer compare = d1.compareTo(d2);
                                        if (compare != 0) {
                                            if (field.getOrder() == OrderDirection.DESC) {
                                                compare = compare * -1;
                                            }
                                            return compare;
                                        }
                                    } else if(m.getReturnType() == Calendar.class) {
                                        Calendar d1 = (Calendar) invoked1;
                                        Calendar d2 = (Calendar) invoked2;
                                        Integer compare = d1.compareTo(d2);
                                        if (compare != 0) {
                                            if (field.getOrder() == OrderDirection.DESC) {
                                                compare = compare * -1;
                                            }
                                            return compare;
                                        }
                                    } else {
                                        String i1 = invoked1.toString();
                                        String i2 = invoked2.toString();
                                        Integer compare = i1.compareTo(i2);
                                        if (compare != 0) {
                                            if (field.getOrder() == OrderDirection.DESC) {
                                                compare = compare * -1;
                                            }
                                            return compare;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                throw new GraphQLException("Unable to sort on provided field. " + e.getMessage());
                            }
                        }
                        return -1;
                    }
                });
            }
        }
        return list;
    }

    private static <T> List<T> filter(List<T> list, Filter f) {
        if (f != null && list.size() > 0) {
            for (FilterField field: f.getFields()) {
                FilterType type = field.getType() != null ? field.getType() : FilterType.STRING;
                list.removeIf(new Predicate<T>() {
                    @Override
                    public boolean test(T t) {
                        try {
                            MethodSelector methodSelector = new MethodSelector(field.getField(), t);
                            Method m = methodSelector.getMethod();
                            Object invoked = m.invoke(methodSelector.getO1());
                            List<String> stringList;
                            switch(field.getOp()) {
                                case EQ:
                                    switch(type) {
                                        case STRING:
                                            return invoked == null || !invoked.toString().equals(field.getValue());
                                        case INTEGER:
                                            return (Integer) invoked != Integer.parseInt(field.getValue());
                                        case FLOAT:
                                            return (Float) invoked != Float.parseFloat(field.getValue());
                                        case DOUBLE:
                                            return (Double) invoked != Double.parseDouble(field.getValue());
                                        case DATE:
                                            Date d1 = (Date)invoked;
                                            DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                                            TemporalAccessor accessor = timeFormatter.parse(field.getValue());
                                            Date d2 = Date.from(Instant.from(accessor));
                                            return d1.compareTo(d2) != 0;
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case EQIC:
                                    switch(type) {
                                        case STRING:
                                            return invoked == null || !invoked.toString().equalsIgnoreCase(field.getValue());
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case NEQ:
                                    switch(type) {
                                        case STRING:
                                            return invoked == null || invoked.toString().equals(field.getValue());
                                        case INTEGER:
                                            return (Integer)invoked == Integer.parseInt(field.getValue());
                                        case FLOAT:
                                            return (Float)invoked == Float.parseFloat(field.getValue());
                                        case DOUBLE:
                                            return (Double)invoked == Double.parseDouble(field.getValue());
                                        case DATE:
                                            Date d1 = (Date)invoked;
                                            DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                                            TemporalAccessor accessor = timeFormatter.parse(field.getValue());
                                            Date d2 = Date.from(Instant.from(accessor));
                                            return d1.compareTo(d2) == 0;
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case NEQIC:
                                    switch(type) {
                                        case STRING:
                                            return invoked == null || invoked.toString().equalsIgnoreCase(field.getValue());
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case LIKE:
                                    switch(type) {
                                        case STRING:
                                            if(invoked != null) {
                                                Pattern pattern = Pattern.compile(field.getValue());
                                                Matcher matcher = pattern.matcher(invoked.toString());
                                                return !matcher.find();
                                            }
                                            return false;
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case LIKEIC:
                                    switch(type) {
                                        case STRING:
                                            if(invoked != null) {
                                                Pattern pattern = Pattern.compile(field.getValue().toLowerCase());
                                                Matcher matcher = pattern.matcher(invoked.toString().toLowerCase());
                                                return !matcher.find();
                                            }
                                            return false;
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case GT:
                                    switch(type) {
                                        case STRING:
                                            return invoked == null || invoked.toString().compareTo(field.getValue()) <= 0;
                                        case INTEGER:
                                            return (Integer) invoked <= Integer.parseInt(field.getValue());
                                        case FLOAT:
                                            return (Float) invoked <= Float.parseFloat(field.getValue());
                                        case DOUBLE:
                                            return (Double) invoked <= Double.parseDouble(field.getValue());
                                        case DATE:
                                            Date d1 = (Date)invoked;
                                            DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                                            TemporalAccessor accessor = timeFormatter.parse(field.getValue());
                                            Date d2 = Date.from(Instant.from(accessor));
                                            return d1.compareTo(d2) <= 0;
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case GTE:
                                    switch(type) {
                                        case STRING:
                                            return invoked == null || invoked.toString().compareTo(field.getValue()) < 0;
                                        case INTEGER:
                                            return (Integer) invoked < Integer.parseInt(field.getValue());
                                        case FLOAT:
                                            return (Float) invoked < Float.parseFloat(field.getValue());
                                        case DOUBLE:
                                            return (Double) invoked < Double.parseDouble(field.getValue());
                                        case DATE:
                                            Date d1 = (Date)invoked;
                                            DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                                            TemporalAccessor accessor = timeFormatter.parse(field.getValue());
                                            Date d2 = Date.from(Instant.from(accessor));
                                            return d1.compareTo(d2) < 0;
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case LT:
                                    switch(type) {
                                        case STRING:
                                            return invoked == null || invoked.toString().compareTo(field.getValue()) >= 0;
                                        case INTEGER:
                                            return (Integer) invoked >= Integer.parseInt(field.getValue());
                                        case FLOAT:
                                            return (Float) invoked >= Float.parseFloat(field.getValue());
                                        case DOUBLE:
                                            return (Double) invoked >= Double.parseDouble(field.getValue());
                                        case DATE:
                                            Date d1 = (Date)invoked;
                                            DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                                            TemporalAccessor accessor = timeFormatter.parse(field.getValue());
                                            Date d2 = Date.from(Instant.from(accessor));
                                            return d1.compareTo(d2) >= 0;
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case LTE:
                                    switch(type) {
                                        case STRING:
                                            return invoked == null || invoked.toString().compareTo(field.getValue()) > 0;
                                        case INTEGER:
                                            return (Integer) invoked > Integer.parseInt(field.getValue());
                                        case FLOAT:
                                            return (Float) invoked > Float.parseFloat(field.getValue());
                                        case DOUBLE:
                                            return (Double) invoked > Double.parseDouble(field.getValue());
                                        case DATE:
                                            Date d1 = (Date)invoked;
                                            DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                                            TemporalAccessor accessor = timeFormatter.parse(field.getValue());
                                            Date d2 = Date.from(Instant.from(accessor));
                                            return d1.compareTo(d2) > 0;
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case IN:
                                    stringList = getStringList(field.getValue(), false);
                                    switch(type) {
                                        case STRING:
                                            if(invoked != null) {
                                                return !stringList.contains(invoked.toString());
                                            }
                                        case INTEGER:
                                            List<Integer> integerList = new ArrayList<>();
                                            for(String item: stringList) {
                                                integerList.add(Integer.parseInt(item));
                                            }
                                            return !integerList.contains(invoked);
                                        case DOUBLE:
                                            List<Double> doubleList = new ArrayList<>();
                                            for(String item: stringList) {
                                                doubleList.add(Double.parseDouble(item));
                                            }
                                            return !doubleList.contains(invoked);
                                        case FLOAT:
                                            List<Float> floatList = new ArrayList<>();
                                            for(String item: stringList) {
                                                floatList.add(Float.parseFloat(item));
                                            }
                                            return !floatList.contains(invoked);
                                        case DATE:
                                            List<Date> dateList = new ArrayList<>();
                                            for(String item: stringList) {
                                                DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                                                TemporalAccessor accessor = timeFormatter.parse(item);
                                                dateList.add(Date.from(Instant.from(accessor)));
                                            }
                                            return !dateList.contains(invoked);
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case INIC:
                                    stringList = getStringList(field.getValue(), true);
                                    switch(type) {
                                        case STRING:
                                            if (invoked != null) {
                                                return !stringList.contains(invoked.toString().toLowerCase());
                                            }
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case NIN:
                                    stringList = getStringList(field.getValue(), false);
                                    switch(type) {
                                        case STRING:
                                            if(invoked != null) {
                                                return stringList.contains(invoked.toString());
                                            }
                                        case INTEGER:
                                            List<Integer> integerList = new ArrayList<>();
                                            for(String item: stringList) {
                                                integerList.add(Integer.parseInt(item));
                                            }
                                            return integerList.contains(invoked);
                                        case DOUBLE:
                                            List<Double> doubleList = new ArrayList<>();
                                            for(String item: stringList) {
                                                doubleList.add(Double.parseDouble(item));
                                            }
                                            return doubleList.contains(invoked);
                                        case FLOAT:
                                            List<Float> floatList = new ArrayList<>();
                                            for(String item: stringList) {
                                                floatList.add(Float.parseFloat(item));
                                            }
                                            return floatList.contains(invoked);
                                        case DATE:
                                            List<Date> dateList = new ArrayList<>();
                                            for(String item: stringList) {
                                                DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                                                TemporalAccessor accessor = timeFormatter.parse(item);
                                                dateList.add(Date.from(Instant.from(accessor)));
                                            }
                                            return dateList.contains(invoked);
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case NINIC:
                                    stringList = getStringList(field.getValue(), true);
                                    switch(type) {
                                        case STRING:
                                            if (invoked != null) {
                                                return stringList.contains(invoked.toString().toLowerCase());
                                            }
                                        default:
                                            throw new GraphQLException("Invalid type provided.");
                                    }
                                case ISNULL:
                                    return invoked != null;
                                case ISNOTNULL:
                                    return invoked == null;
                            }
                            return false;
                        } catch(Exception e) {
                            throw new GraphQLException("Unable to sort. " + e.getMessage());
                        }
                    }
                });
            }
        }
        return list;
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