package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.PersistentEntityInformation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

/**
 * Creates repository implementation based on Mybatis.
 */
public class MybatisRepositoryFactory extends RepositoryFactorySupport {

    private final RelationalMappingContext context;
    private final SqlSessionFactory sqlSessionFactory;

    /**
     * Creates a new {@link MybatisRepositoryFactory} for the given
     * {@link RelationalMappingContext} and {@link SqlSessionFactory}
     *
     * @param context must not be {@literal null}.
     * @param sqlSessionFactory must not be {@literal null}.
     */
    public MybatisRepositoryFactory(RelationalMappingContext context, SqlSessionFactory sqlSessionFactory) {

        Assert.notNull(context, "RelationalMappingContext must not be null!");
        Assert.notNull(sqlSessionFactory, "SqlSessionFactory must not be null!");

        this.context = context;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> aClass) {

        RelationalPersistentEntity<?> entity = context.getRequiredPersistentEntity(aClass);

        return (EntityInformation<T, ID>) new PersistentEntityInformation<>(entity);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryInformation)
     */
    @Override
    protected Object getTargetRepository(RepositoryInformation repositoryInformation) {
        Class<?> mapperClass = repositoryInformation.getRepositoryInterface();
        org.apache.ibatis.session.Configuration config = sqlSessionFactory.getConfiguration();
        MybatisRepositoryConfigurer.configure(config, mapperClass, repositoryInformation.getDomainType());
        
        MapperFactoryBean<?> mapperFactoryBean = new MapperFactoryBean<>(mapperClass);
        mapperFactoryBean.setSqlSessionFactory(sqlSessionFactory);
        mapperFactoryBean.afterPropertiesSet();
        try {
            return mapperFactoryBean.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepositoryBaseClass(org.springframework.data.repository.core.RepositoryMetadata)
     */
    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
        // here we return repository interface other than MybatisRepository, 
        // because we want all method delegate to mybatis MapperProxy
        return repositoryMetadata.getRepositoryInterface();
    }

}
