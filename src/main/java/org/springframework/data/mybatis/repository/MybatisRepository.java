package org.springframework.data.mybatis.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Table;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Persistable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@NoRepositoryBean
@Transactional(readOnly = true)
public interface MybatisRepository<T extends Persistable<ID>, ID> extends PagingAndSortingRepository<T, ID> {
    
    String DEFAULT_RESULTMAP = "defaultResultMap";
    String SCRIPT_BEGIN = "<script>";
    String SCRIPT_END = "</script>";
    String FOREACH_ITEMS = "<foreach item='item' collection='items' open='(' separator=',' close=')'>#{item}</foreach>";
    
    Map<Class<?>, String> TABLE_MAP = new HashMap<>();
    
    default String table() {
        Class<?> repositoryClass = getClass();
        String tableName = TABLE_MAP.get(repositoryClass);
        if (tableName == null) {
            Class<?> entityType = GenericTypeResolver.resolveTypeArguments(repositoryClass, MybatisRepository.class)[0];
            Table table = AnnotationUtils.findAnnotation(entityType, Table.class);
            if (table == null) {
                String message = String.format("Entity %s must be annotated with @javax.persistence.Table", 
                        entityType.getName());
                throw new IllegalStateException(message);
            }
            tableName = table.name();
            TABLE_MAP.put(repositoryClass, tableName);
        }
        return tableName;
    }
    
    @Select("select o.* from ${table} o where o.id = #{id}")
    @ResultMap(DEFAULT_RESULTMAP)
    T findById(@Param("table") String table, @Param("id") ID id);
    
    @Select("select o.* from ${table} o")
    @ResultMap(DEFAULT_RESULTMAP)
    Page<T> findWithPageable(@Param("table") String table, Pageable pageable);
    
    @Select("select o.* from ${table} o order by ${sorts}")
    @ResultMap(DEFAULT_RESULTMAP)
    List<T> findWithSort(@Param("table") String table, @Param("sorts") String sorts);
    
    @Select(SCRIPT_BEGIN + "select o.* from ${table} o where o.id in " + FOREACH_ITEMS + SCRIPT_END)
    @ResultMap(DEFAULT_RESULTMAP)
    List<T> findByIds(@Param("table") String table, @Param("items") Iterable<ID> ids);
    
    @Select("select * from ${table}")
    @ResultMap(DEFAULT_RESULTMAP)
    List<T> findTable(@Param("table") String table);
    
    @Transactional
    <S extends T> int insert(@Param("table") String table, @Param("entity") S entity);

    @Transactional
    <S extends T> int update(@Param("table") String table, @Param("entity") S entity);
    
    @Select("select count(id) from ${table} where id = #{id}")
    @ResultType(long.class)
    long countById(@Param("table") String table, @Param("id") ID id);

    @Select("select count(id) from ${table}")
    @ResultType(long.class)
    long countTable(@Param("table") String table);
    
    @Transactional
    @Delete("delete from ${table} where id = #{id}")
    int deleteById(@Param("table") String table, @Param("id") ID id);
    
    @Transactional
    @Delete(SCRIPT_BEGIN + "delete from ${table} where id in " + FOREACH_ITEMS + SCRIPT_END)
    int deleteByIds(@Param("table") String table, @Param("items") Iterable<ID> ids);
    
    @Transactional
    @Delete(value = "delete from ${table}")
    int deleteTable(@Param("table") String table);
    
    @Override
    default Optional<T> findById(ID id) {
        if (id != null) {
            return Optional.ofNullable(findById(table(), id));
        }
        return Optional.empty();
    }

    @Override
    default Page<T> findAll(Pageable pageable) {
        return findWithPageable(table(), pageable);
    }

    @Override
    default List<T> findAll(Sort sort) {
        if (sort != null) {
            return findWithSort(table(), orderByFragment(sort));
        }
        return findAll();
    }
    
    static String orderByFragment(Sort sort) {
        if (sort != null && sort.isSorted()) {
            Iterator<Order> orderIter = sort.iterator();
            List<String> sorts = new ArrayList<>();
            while (orderIter.hasNext()) {
                Order order = orderIter.next();
                StringBuilder sortBuf = new StringBuilder();
                sortBuf.append("o.").append(order.getProperty()).append(" ").append(order.getDirection().name());
                sorts.add(sortBuf.toString());
            }
            if (sorts.size() > 0) {
                return StringUtils.collectionToCommaDelimitedString(sorts);
            }
        }
        return "";
    }
    
    @Override
    default List<T> findAll() {
        return findTable(table());
    }
    
    @Override
    default List<T> findAllById(Iterable<ID> ids) {
        if (ids == null || !ids.iterator().hasNext()) {
            return Collections.emptyList();
        }
        return findByIds(table(), ids);
    }
    
    @Override
    default boolean existsById(ID id) {
        Assert.notNull(id, "The given id must not be null.");
        return countById(table(), id) > 0;
    }

    @Override
    default long count() {
        return countTable(table());
    }

    @Override
    @Transactional
    default <S extends T> S save(S entity) {
        Assert.notNull(entity, "The given entity must not be null.");
        ID id = entity.getId();
        if (id == null) {
            insert(table(), entity);
        } else {
            update(table(), entity);
        }
        return entity;
    }
    
    @Override
    @Transactional
    default <S extends T> List<S> saveAll(Iterable<S> entities) {
        List<S> result = new ArrayList<S>();
        if (entities == null) {
            return result;
        }
        for (S entity : entities) {
            result.add(save(entity));
        }
        return result;
    }
    
    @Override
    @Transactional
    default void deleteById(ID id) {
        deleteById(table(), id);
    }

    @Override
    @Transactional
    default void delete(T entity) {
        Assert.notNull(entity, "The entity must not be null!");
        ID id = entity.getId();
        if (id != null) {
            deleteById(id);
        }
    }

    @Override
    @Transactional
    default void deleteAll(Iterable<? extends T> entities) {
        Assert.notNull(entities, "The given Iterable of entities not be null!");
        Set<ID> ids = new LinkedHashSet<>();
        for (T entity : entities) {
            ID id = entity.getId();
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.size() > 0) {
            deleteByIds(table(), ids);
        }
    }
    
    @Override
    @Transactional
    default void deleteAll() {
        deleteTable(table());
    }
    
}
