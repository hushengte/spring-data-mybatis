package org.springframework.data.mybatis.repository.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.StringUtils;

@Intercepts(@Signature(type = Executor.class, method = "query", args = {
    MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
}))
public class PageableInteceptor implements Interceptor {
    
    public PageableInteceptor() {}
    
    private static Pageable findPageable(Object parameterObject, boolean isMap) {
        if (parameterObject != null) {
            if (parameterObject instanceof Pageable) {
                return (Pageable)parameterObject;
            }
            if (isMap) {
                Map<?, ?> queryParams = (Map<?, ?>)parameterObject;
                for (Map.Entry<?, ?> entry : queryParams.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Pageable) {
                        return (Pageable)value;
                    }
                }
            }
        }
        return null;
    }
    
    private static MappedStatement buildPageableStatement(MappedStatement ms, SqlSource pageableSqlSource, String pageableId) {
        org.apache.ibatis.session.Configuration configuration = ms.getConfiguration();
        String pageableStatementId = ms.getId() + "!pageable-" + pageableId;
        if (configuration.hasStatement(pageableStatementId, false)) {
            return configuration.getMappedStatement(pageableStatementId);
        }
        MappedStatement.Builder builder = new MappedStatement.Builder(
                configuration, pageableStatementId, pageableSqlSource, ms.getSqlCommandType());
        builder.resultMaps(ms.getResultMaps());
        MappedStatement pageableStatement = builder.build();
        configuration.addMappedStatement(pageableStatement);
        return pageableStatement;
    }
    
    private static MappedStatement buildCountStatement(MappedStatement ms, StaticSqlSource countSqlSource, String countId) {
        org.apache.ibatis.session.Configuration configuration = ms.getConfiguration();
        String statementId = ms.getId() + "!count-" + countId;
        if (configuration.hasStatement(statementId, false)) {
            return configuration.getMappedStatement(statementId);
        }
        MappedStatement.Builder builder = new MappedStatement.Builder(
                configuration, statementId, countSqlSource, ms.getSqlCommandType());
        ResultMap inlineResultMap = new ResultMap.Builder(configuration, statementId + "-Inline",
                Long.class, new ArrayList<>()).build();
        builder.resultMaps(Collections.singletonList(inlineResultMap));
        MappedStatement countStatement = builder.build();
        configuration.addMappedStatement(countStatement);
        return countStatement;
    }
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        Object parameterObject = args[1];
        boolean isMap = parameterObject instanceof Map;
        Pageable pageable = findPageable(parameterObject, isMap);
        if (pageable == null) {
            return invocation.proceed();
        }
        if (pageable.isUnpaged()) {
            Page<?> pageData = new PageImpl<>((List<?>)invocation.proceed(), pageable, 0);
            return Collections.singletonList(pageData);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> queryParams = isMap ? (Map<String, Object>)parameterObject : new HashMap<>();
        MappedStatement ms = (MappedStatement) args[0];
        org.apache.ibatis.session.Configuration configuration = ms.getConfiguration();
        BoundSql boundSql = ms.getBoundSql(queryParams);
        String originalSql = boundSql.getSql().toLowerCase();

        long total = 0;
        String countSql = buildCountSql(originalSql);
        if (StringUtils.hasText(countSql)) {
            StaticSqlSource countSqlSource = new StaticSqlSource(configuration, countSql, boundSql.getParameterMappings());
            args[0] = buildCountStatement(ms, countSqlSource, String.valueOf(countSql.hashCode()));
            args[1] = queryParams;
            List<?> countResult = (List<?>)invocation.proceed();
            total = (Long)countResult.get(0);
        }
        List<?> content = Collections.emptyList();
        if (total > 0) {
            queryParams.put("offset", (int)pageable.getOffset());
            queryParams.put("size", pageable.getPageSize());
            
            List<ParameterMapping> parameterMappings = new ArrayList<>();
            parameterMappings.addAll(boundSql.getParameterMappings());
            parameterMappings.add(new ParameterMapping.Builder(configuration, "offset", int.class).build());
            parameterMappings.add(new ParameterMapping.Builder(configuration, "size", int.class).build());
            String pageableSql = buildPageableSql(originalSql, pageable);
            StaticSqlSource pageableSqlSource = new StaticSqlSource(configuration, pageableSql, parameterMappings);
            
            args[0] = buildPageableStatement(ms, pageableSqlSource, String.valueOf(pageableSql.hashCode()));
            args[1] = queryParams;
            content = (List<?>) invocation.proceed();
        }
        Page<?> pageData = new PageImpl<>(content, pageable, total);
        return Collections.singletonList(pageData);
    }
    
    private static String buildCountSql(String originalSql) {
        int fromKeyworkIndex = originalSql.indexOf("from");
        if (fromKeyworkIndex > 0) {
            int endIndex = originalSql.length();
            int limitKeywordIndex = originalSql.lastIndexOf("limit");
            if (limitKeywordIndex > 0) {
                endIndex = limitKeywordIndex;
            }
            StringBuilder countSqlBuf = new StringBuilder("select count(*) ");
            countSqlBuf.append(originalSql.substring(fromKeyworkIndex, endIndex));
            return countSqlBuf.toString();
        }
        return null;
    }
    
    private static String buildPageableSql(String originalSql, Pageable pageable) {
        StringBuilder sqlBuf = new StringBuilder(originalSql);
        String sortFragment = buildSortFragment(pageable.getSort());
        if (sortFragment.length() > 0) {
            sqlBuf.append(" order by ").append(sortFragment);
        }
        sqlBuf.append(" limit ?,?");
        String sql = sqlBuf.toString();
        return sql;
    }
    
    /**
     * Construct "order by" sql fragment for {@link Sort}, column name will be prefixed with "o." automatically.
     * @param sort Sort object
     * @return "order by" sql fragment
     */
    public static String buildSortFragment(Sort sort) {
        if (sort != null && sort.isSorted()) {
            Iterator<Order> orderIter = sort.iterator();
            List<String> sorts = new ArrayList<>();
            while (orderIter.hasNext()) {
                Order order = orderIter.next();
                StringBuilder sortBuf = new StringBuilder("o.");
                sortBuf.append(order.getProperty()).append(" ").append(order.getDirection().name());
                sorts.add(sortBuf.toString());
            }
            if (sorts.size() > 0) {
                return StringUtils.collectionToCommaDelimitedString(sorts);
            }
        }
        return "";
    }
    
}
