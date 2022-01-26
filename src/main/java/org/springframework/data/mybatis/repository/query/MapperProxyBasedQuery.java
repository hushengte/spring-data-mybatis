package org.springframework.data.mybatis.repository.query;

import java.lang.reflect.Method;

import org.springframework.util.ReflectionUtils;

/**
 * A query to be executed based on a repository method, 
 * which is a normally a mybatis mapper method.
 */
public class MapperProxyBasedQuery extends AbstractMybatisQuery {

    private final Object mapperProxyTarget;
    
    /**
     * Creates a new {@link MapperProxyBasedQuery}
     * 
     * @param queryMethod must not be {@literal null}.
     * @param mapperProxyTarget must not be {@literal null}.
     */
    public MapperProxyBasedQuery(MybatisQueryMethod queryMethod, Object mapperProxyTarget) {
        super(queryMethod);
        this.mapperProxyTarget = mapperProxyTarget;
    }

    @Override
    public Object execute(Object[] parameters) {
        Method method = getQueryMethod().getMethod();
        return ReflectionUtils.invokeMethod(method, mapperProxyTarget, parameters);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.RepositoryQuery#getQueryMethod()
     */
    @Override
    public MybatisQueryMethod getQueryMethod() {
        return (MybatisQueryMethod) super.getQueryMethod();
    }
    
}
