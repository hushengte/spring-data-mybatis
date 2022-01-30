package org.springframework.data.mybatis.dao;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mybatis.domain.Book;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface BookDao extends MybatisRepository<Book, Integer> {

    @Query
    @Select("select b.id, b.name, b.author, b.isbn, b.call_number, b.publish_year, b.serial_name, b.ebook, b.publisher_id,"
            + " p.name as publisher_name, p.place as publisher_place from lib_book b"
            + " left join lib_publisher p on b.publisher_id = p.id where b.id = #{id}")
    @ResultMap(DEFAULT_RESULTMAP)
	Book getById(@Param("id") Integer id);
    
    @Query
    @Select("select o.name from lib_book o where o.id = #{id}")
    @ResultType(String.class)
    String findNameById(@Param("id") Integer id);
	
    default Page<Book> findByOrderByLastUpdateDesc(Pageable pageable) {
        Sort sort = Sort.by(Direction.DESC, "last_update");
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return findWithPageableFetchPublisher(pageRequest);
    }
    
    default Page<Book> findByIsbnContainingOrderByLastUpdateDesc(String keyword, Pageable pageable) {
        Sort sort = Sort.by(Direction.DESC, "last_update");
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return findByIsbnLikeWithPageableFetchPublisher(keyword, pageRequest);
    }
    
    @Query
    @Transactional
    @Update("update lib_book o set o.ebook = #{ebookName} where o.id = #{bookId}")
    int updateEbook(@Param("bookId") Integer bookId, @Param("ebookName") String ebookName);
    
    @Query
    @Select("select o.*, p.name as publisher_name, p.place as publisher_place from lib_book o"
            + " left join lib_publisher p on o.publisher_id = p.id")
    @ResultMap(DEFAULT_RESULTMAP)
    Page<Book> findWithPageableFetchPublisher(Pageable pageable);
    
    @Query
    @Select("select o.*, p.name as publisher_name, p.place as publisher_place from lib_book o"
            + " left join lib_publisher p on o.publisher_id = p.id where o.isbn like concat('%',#{keyword},'%')")
    @ResultMap(DEFAULT_RESULTMAP)
    Page<Book> findByIsbnLikeWithPageableFetchPublisher(@Param("keyword") String keyword, Pageable pageable);

}
