package org.springframework.data.mybatis.repository.support;

import java.io.Serializable;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.relational.core.dialect.Dialect;
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
    private Dialect dialect;
    private SqlSessionTemplate sqlSessionTemplate;

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

        return new MybatisRepositoryFactory(mappingContext, dialect, sqlSessionTemplate);
    }

    @Autowired
    protected void setMappingContext(RelationalMappingContext mappingContext) {

        Assert.notNull(mappingContext, "MappingContext must not be null");

        super.setMappingContext(mappingContext);
        this.mappingContext = mappingContext;
    }
    
    @Autowired
    protected void setDialect(Dialect dialect) {

        Assert.notNull(dialect, "Dialect must not be null");

        this.dialect = dialect;
    }
    
    @Autowired
    protected void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {

        Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null");

        this.sqlSessionTemplate = sqlSessionTemplate;
    }

}
