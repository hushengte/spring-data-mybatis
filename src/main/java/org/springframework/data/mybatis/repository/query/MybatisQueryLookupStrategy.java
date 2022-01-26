package org.springframework.data.mybatis.repository.query;

import java.lang.reflect.Method;

import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

/**
 * {@link QueryLookupStrategy} for Mybatis repositories.
 */
public class MybatisQueryLookupStrategy implements QueryLookupStrategy {
    
    private final SqlSessionTemplate sqlSessionTemplate;
    
    private Object mapperProxyTarget;
    
    public MybatisQueryLookupStrategy(SqlSessionTemplate sqlSessionTemplate) {
        Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null");
        this.sqlSessionTemplate = sqlSessionTemplate;
    }
    
    private Object getMapperProxyTarget(RepositoryMetadata metadata) throws Exception {
        if (mapperProxyTarget == null) {
            MapperFactoryBean<?> mapperFactoryBean = new MapperFactoryBean<>(metadata.getRepositoryInterface());
            mapperFactoryBean.setSqlSessionTemplate(sqlSessionTemplate);
            mapperFactoryBean.afterPropertiesSet();
            mapperProxyTarget = mapperFactoryBean.getObject();
        }
        return mapperProxyTarget;
    }
    
    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, 
     * org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, 
     * org.springframework.data.repository.core.NamedQueries)
     */
    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, 
            ProjectionFactory factory, NamedQueries namedQueries) {
        MybatisQueryMethod queryMethod = new MybatisQueryMethod(method, metadata, factory);
        try {
            Object mapperProxyTarget = getMapperProxyTarget(metadata);
            return new MapperProxyBasedQuery(queryMethod, mapperProxyTarget);
        } catch (Exception e) {
            throw QueryCreationException.create(queryMethod, e.getMessage());
        }
        
    }

}
