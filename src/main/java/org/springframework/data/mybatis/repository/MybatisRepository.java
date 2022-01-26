package org.springframework.data.mybatis.repository;

import java.util.List;

import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
@Transactional(readOnly = true)
public interface MybatisRepository<T, ID> extends PagingAndSortingRepository<T, ID> {
    
    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findAll(java.lang.Iterable)
     */
    @Override
    List<T> findAllById(Iterable<ID> ids);
    
    /**
     * Acquire database lock with the given id.
     * 
     * @param id identifier of entity
     * @param lockMode database lock mode
     * @return identifier of entity
     */
    ID lockById(ID id, LockMode lockMode);
    
}
