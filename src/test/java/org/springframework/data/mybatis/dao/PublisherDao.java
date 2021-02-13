package org.springframework.data.mybatis.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mybatis.domain.Publisher;
import org.springframework.data.mybatis.repository.MybatisRepository;

public interface PublisherDao extends MybatisRepository<Publisher, Integer> {
    
    @Select("select o.* from lib_publisher o where o.name = #{name}")
    @ResultMap(DEFAULT_RESULTMAP)
    List<Publisher> findByName(String name);

    @Select("select o.* from lib_publisher o where o.name like concat('%',#{nameKeyword},'%')")
    @ResultMap(DEFAULT_RESULTMAP)
    Page<Publisher> findByNameContaining(@Param("nameKeyword") String nameKeyword, Pageable pageable);

    @Select("select o.* from lib_publisher o where o.place like concat('%',#{placeKeyword},'%')")
    @ResultMap(DEFAULT_RESULTMAP)
    Page<Publisher> findByPlaceContaining(@Param("placeKeyword") String placeKeyword, Pageable pageable);
    
}
