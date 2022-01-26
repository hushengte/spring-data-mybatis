package org.springframework.data.mybatis.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mybatis.domain.Publisher;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.Query;
import org.springframework.data.mybatis.statement.Statement;

public interface PublisherDao extends MybatisRepository<Publisher, Integer> {
    
    @Query
    @Select("select o.* from lib_publisher o where o.name = #{name}")
    @ResultMap(Statement.RESULTMAP_DEFAULT)
    List<Publisher> findByName(String name);

    @Query
    @Select("select o.* from lib_publisher o where o.name like concat('%',#{nameKeyword},'%')")
    @ResultMap(Statement.RESULTMAP_DEFAULT)
    Page<Publisher> findByNameContaining(@Param("nameKeyword") String nameKeyword, Pageable pageable);

    @Query
    @Select("select o.* from lib_publisher o where o.place like concat('%',#{placeKeyword},'%')")
    @ResultMap(Statement.RESULTMAP_DEFAULT)
    Page<Publisher> findByPlaceContaining(@Param("placeKeyword") String placeKeyword, Pageable pageable);
    
}
