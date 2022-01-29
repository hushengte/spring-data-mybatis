package org.springframework.data.mybatis.repository.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mybatis.statement.page.Count;
import org.springframework.data.mybatis.statement.page.OrderByAndLimit;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

@Intercepts(@Signature(type = Executor.class, method = "query", args = {
    MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
}))
public class PageableInteceptor implements Interceptor {
    
    private final Dialect dialect;
    
    public PageableInteceptor(Dialect dialect) {
        Assert.notNull(dialect, "Dialect must not be null.");
        this.dialect = dialect;
    }
    
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
        String originalSql = boundSql.getSql();
        
        // configure count statement
        args[0] = new Count(ms.getId(), originalSql, boundSql.getParameterMappings())
                .configure(configuration, null, null, null);
        args[1] = queryParams;
        
        // Execute the count statement to get total number
        List<?> countResult = (List<?>)invocation.proceed();
        long total = CollectionUtils.isEmpty(countResult) ? 0 : (Long)countResult.get(0);
        
        List<?> content = Collections.emptyList();
        if (total > 0) {
            // add pageable parameters
            queryParams.put(OrderByAndLimit.PARAM_KEY_OFFSET, (int)pageable.getOffset());
            queryParams.put(OrderByAndLimit.PARAM_KEY_SIZE, pageable.getPageSize());
            
            // configure the pageable statement and get the result rows
            args[0] = new OrderByAndLimit(ms.getId(), originalSql,
                    boundSql.getParameterMappings(), ms.getResultMaps(), pageable, dialect)
                    .configure(configuration, null, null, null);
            args[1] = queryParams;
            content = (List<?>) invocation.proceed();
        }
        Page<?> pageData = new PageImpl<>(content, pageable, total);
        return Collections.singletonList(pageData);
    }
    
}
