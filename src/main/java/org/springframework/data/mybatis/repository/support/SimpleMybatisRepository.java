package org.springframework.data.mybatis.repository.support;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Persistable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.statement.Statement;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.util.Streamable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link MybatisRepository} interface.
 */
@Transactional(readOnly = true)
public class SimpleMybatisRepository<T extends Persistable<?>, ID> implements MybatisRepository<T, ID> {

    private final SqlSessionTemplate sqlSessionTemplate;
    private final String namespace;
    
    public SimpleMybatisRepository(SqlSessionTemplate sqlSessionTemplate, Class<?> repositoryType) {
        Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate is required.");
        Assert.notNull(repositoryType, "repositoryType is required.");
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.namespace = repositoryType.getName();
    }
    
    private String namespace(String id) {
        return new StringBuilder(namespace).append(Statement.DOT).append(id).toString();
    }
    
    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#save(S)
     */
    @Override
    @Transactional
    public <S extends T> S save(S instance) {
        Assert.notNull(instance, "The given instance must not be null.");
        Object id = instance.getId();
        if (id == null) {
            sqlSessionTemplate.insert(namespace(Statement.INSERT), instance);
        } else {
            sqlSessionTemplate.update(namespace(Statement.UPDATE_BY_ID), instance);
        }
        return instance;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#save(java.lang.Iterable)
     */
    @Override
    @Transactional
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        return Streamable.of(entities).stream()
                .map(this::save)
                .collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findOne(java.io.Serializable)
     */
    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sqlSessionTemplate.selectOne(namespace(Statement.FIND_BY_ID), id));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#exists(java.io.Serializable)
     */
    @Override
    public boolean existsById(ID id) {
        if (id == null) {
            return false;
        }
        long count = sqlSessionTemplate.selectOne(namespace(Statement.COUNT_BY_ID), id);
        return count > 0;
    }
    
    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#count()
     */
    @Override
    public long count() {
        return sqlSessionTemplate.selectOne(namespace(Statement.COUNT_ALL));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findAll()
     */
    @Override
    public List<T> findAll() {
        return sqlSessionTemplate.selectList(namespace(Statement.FIND_ALL));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findAll(java.lang.Iterable)
     */
    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        if (ids == null || !ids.iterator().hasNext()) {
            return Collections.emptyList();
        }
        return sqlSessionTemplate.selectList(namespace(Statement.FIND_BY_IDS), ids);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.PagingAndSortingRepository#findAll(org.springframework.data.domain.Sort sort)
     */
    @Override
    public List<T> findAll(Sort sort) {
        return sqlSessionTemplate.selectList(namespace(Statement.FIND_ALL), sort);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.PagingAndSortingRepository#findAll(org.springframework.data.domain.Pageable pageable)
     */
    @Override
    public Page<T> findAll(Pageable pageable) {
        return sqlSessionTemplate.selectOne(namespace(Statement.FIND_ALL), pageable);
    }
    
    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#delete(java.io.Serializable)
     */
    @Override
    @Transactional
    public void deleteById(Object id) {
        sqlSessionTemplate.delete(namespace(Statement.DELETE_BY_ID), id);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#delete(java.lang.Object)
     */
    @Override
    @Transactional
    public void delete(T instance) {
        if (instance != null) {
            deleteById(instance.getId());
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#deleteAllById(java.lang.Iterable)
     */
    @Override
    @Transactional
    public void deleteAllById(Iterable<? extends ID> ids) {
        if (ids == null || !ids.iterator().hasNext()) {
            return;
        }
        sqlSessionTemplate.delete(namespace(Statement.DELETE_BY_IDS), ids);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#delete(java.lang.Iterable)
     */
    @Override
    @Transactional
    public void deleteAll(Iterable<? extends T> entities) {
        if (entities != null) {
            List<Object> ids = Streamable.of(entities).stream()
                .map(entity -> entity.getId())
                .collect(Collectors.toList());
            sqlSessionTemplate.delete(namespace(Statement.DELETE_BY_IDS), ids);
        }
    }

    @Override
    @Transactional
    public void deleteAll() {
        sqlSessionTemplate.delete(namespace(Statement.DELETE_ALL));
    }

    @Override
    public ID lockById(ID id, LockMode lockMode) {
        Assert.notNull(id, "The given id must not be null.");
        
        String statmentId = LockMode.PESSIMISTIC_READ.equals(lockMode) ? Statement.READ_LOCK_BY_ID:
            Statement.WRITE_LOCK_BY_ID;
        return sqlSessionTemplate.selectOne(namespace(statmentId), id);
    }

}
