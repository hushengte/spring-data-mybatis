package org.springframework.data.mybatis.repository.query;

import java.lang.reflect.Method;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

/**
 * {@link QueryMethod} implementation that executing a mybatis query
 */
public class MybatisQueryMethod extends QueryMethod {
    
    private final Method method;

    public MybatisQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
        super(method, metadata, factory);
        this.method = method;
    }

    Method getPlainMethod() {
        return method;
    }

}
