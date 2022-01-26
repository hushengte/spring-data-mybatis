package org.springframework.data.mybatis.repository;

import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mybatis.config.MybatisConfig;
import org.springframework.data.mybatis.dao.BookDao;
import org.springframework.data.mybatis.domain.Book;
import org.springframework.data.mybatis.domain.Publisher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {MybatisConfig.class})
@Sql(scripts = {"/data.sql"}, config = @SqlConfig(encoding = "UTF-8"))
public class BookDaoTest {
    
    @Autowired
    private BookDao bookDao;
    
    @Test
    public void testFindById() {
        Optional<Book> bookOp = bookDao.findById(1);
        assertTrue(bookOp.isPresent());
        
        Book book = bookOp.get();
        Publisher publisher = book.getPublisher();
        assertTrue(publisher != null);
        assertTrue(publisher.getId().equals(1));
    }
    
}
