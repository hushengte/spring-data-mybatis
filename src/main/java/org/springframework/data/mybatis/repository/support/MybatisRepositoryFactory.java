package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mybatis.statement.Statements;
import org.springframework.data.relational.core.dialect.Dialect;
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

    private final RelationalMappingContext mappingContext;
    private final Dialect dialect;
    private final SqlSessionTemplate sqlSessionTemplate;

    /**
     * Creates a new {@link MybatisRepositoryFactory} for the given
     * {@link RelationalMappingContext} and {@link SqlSessionFactory}
     *
     * @param mappingContext must not be {@literal null}.
     * @param dialect must not be {@literal null}.
     * @param sqlSessionTemplate must not be {@literal null}.
     */
    public MybatisRepositoryFactory(RelationalMappingContext mappingContext, Dialect dialect, SqlSessionTemplate sqlSessionTemplate) {

        Assert.notNull(mappingContext, "RelationalMappingContext must not be null!");
        Assert.notNull(dialect, "Dialect must not be null!");
        Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");

        this.mappingContext = mappingContext;
        this.dialect = dialect;
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> aClass) {
        RelationalPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(aClass);
        return (EntityInformation<T, ID>) new PersistentEntityInformation<>(entity);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryInformation)
     */
    @Override
    protected Object getTargetRepository(RepositoryInformation repositoryInformation) {
        Class<?> domainType = repositoryInformation.getDomainType();
        Class<?> repositoryType = repositoryInformation.getRepositoryInterface();
        
        Statements.configure(sqlSessionTemplate.getConfiguration(), repositoryType, domainType, mappingContext, dialect);
        return new SimpleMybatisRepository<>(sqlSessionTemplate, repositoryType);
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
