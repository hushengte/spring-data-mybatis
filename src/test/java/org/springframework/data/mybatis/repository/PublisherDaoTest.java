package org.springframework.data.mybatis.repository;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mybatis.config.MybatisConfig;
import org.springframework.data.mybatis.dao.PublisherDao;
import org.springframework.data.mybatis.domain.Publisher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {MybatisConfig.class})
@Sql(scripts = {"/data.sql"}, config = @SqlConfig(encoding = "UTF-8"))
public class PublisherDaoTest {
    
    @Autowired
    private PublisherDao publisherDao;
    
    @Test
    public void testSaveAndDelete() {
        Publisher p = new Publisher("test-name", "test-place");
        Publisher saved = publisherDao.save(p);
        assertNotNull(saved.getId());
        
        Publisher entity = publisherDao.findById(saved.getId()).get();
        assertEquals(p.getName(), entity.getName());
        assertEquals(p.getPlace(), entity.getPlace());
        
        entity.setName("update-name");
        entity.setName("update-place");
        publisherDao.save(entity);
        Publisher updated = publisherDao.findById(entity.getId()).get();
        assertEquals(entity.getName(), updated.getName());
        assertEquals(entity.getPlace(), updated.getPlace());
        
        publisherDao.delete(entity);
        assertTrue(!publisherDao.existsById(entity.getId()));
        
        assertEquals(7L, publisherDao.count());
    }
    
    @Test
    public void testFindWithLikeAndInClause() {
        List<Publisher> items = new ArrayList<>();
        items.add(new Publisher("publisher-aaa", "wenzhou"));
        items.add(new Publisher("publisher-bbb", "ningbo"));
        items.add(new Publisher("publisher-aaa", "hangzhou"));
        publisherDao.saveAll(items);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Publisher> pageData = publisherDao.findByNameContaining("publisher", pageable);
        assertEquals(3L, pageData.getTotalElements());
        assertArrayEquals(items.toArray(), pageData.getContent().toArray());
        
        pageData = publisherDao.findByPlaceContaining("zhou", pageable);
        items.remove(1);
        assertEquals(2L, pageData.getTotalElements());
        assertArrayEquals(items.toArray(), pageData.getContent().toArray());
        
        List<Integer> ids = items.stream().map(item -> item.getId()).collect(Collectors.toList());
        List<Publisher> publishers = publisherDao.findAllById(ids);
        assertEquals(2, publishers.size());
        assertArrayEquals(items.toArray(), publishers.toArray());
        
        publisherDao.deleteAll(publishers);
        assertEquals(8L, publisherDao.count());
    }
    
}
