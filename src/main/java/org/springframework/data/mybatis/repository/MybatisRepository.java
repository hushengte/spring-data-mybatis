package org.springframework.data.mybatis.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.statement.Statement;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
@Transactional(readOnly = true)
public interface MybatisRepository<T, ID> extends PagingAndSortingRepository<T, ID> {
    
    String DEFAULT_RESULTMAP = "resultMap[default]";
    String FOREACH_ITEMS = "<foreach collection='items' item='item' separator=',' open='(' close=')'>#{item}</foreach>";
    String SCRIPT_BEGIN = Statement.SCRIPT_BEGIN;
    String SCRIPT_END = Statement.SCRIPT_END;
    
    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#saveAll(java.lang.Iterable)
     */
    @Override
    <S extends T> List<S> saveAll(Iterable<S> entities);
    
    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findAll()
     */
    @Override
    List<T> findAll();
    
    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findAllById(java.lang.Iterable)
     */
    @Override
    List<T> findAllById(Iterable<ID> ids);
    
    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.PagingAndSortingRepository#findAll(org.springframework.data.domain.Sort)
     */
    @Override
    List<T> findAll(Sort sort);
    
    /**
     * Acquire database lock with the given id.
     * 
     * @param id identifier of entity
     * @param lockMode database lock mode
     * @return identifier of entity
     */
    ID lockById(ID id, LockMode lockMode);
    
}
