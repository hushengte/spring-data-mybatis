package org.springframework.data.mybatis.repository.support;

import java.io.Serializable;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.util.Assert;

/**
 * Special adapter for Springs {@link org.springframework.beans.factory.FactoryBean} interface to allow easy setup of
 * repository factories via Spring configuration.
 */
public class MybatisRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
        extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> implements ApplicationEventPublisherAware {

    private RelationalMappingContext mappingContext;
    private SqlSessionFactory sqlSessionFactory;

    /**
     * Creates a new {@link MybatisRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    protected MybatisRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    /**
     * Creates the actual {@link RepositoryFactorySupport} instance.
     */
    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {

        return new MybatisRepositoryFactory(mappingContext, sqlSessionFactory);
    }

    @Autowired
    protected void setMappingContext(RelationalMappingContext mappingContext) {

        Assert.notNull(mappingContext, "MappingContext must not be null");

        super.setMappingContext(mappingContext);
        this.mappingContext = mappingContext;
    }
    
    @Autowired
    protected void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {

        Assert.notNull(sqlSessionFactory, "SqlSessionFactory must not be null");

        this.sqlSessionFactory = sqlSessionFactory;
    }

}
