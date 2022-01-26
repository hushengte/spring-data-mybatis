package org.springframework.data.mybatis.repository.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mybatis.repository.query.PageableInteceptor;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.MySqlDialect;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Beans that must be registered for Spring Data Mybatis to work.
 */
@Configuration(proxyBeanMethods = false)
public class DefaultMybatisConfiguration {

    @Bean
    public org.apache.ibatis.session.Configuration mybatisConfiguration() {
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        config.setAutoMappingBehavior(AutoMappingBehavior.PARTIAL);
        config.addInterceptor(new PageableInteceptor());
        return config;
    }
    
    @Bean
    public SqlSessionFactory sqlSessionFactory(org.apache.ibatis.session.Configuration config,
            DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setConfiguration(config);
        factory.setDataSource(dataSource);
        return factory.getObject();
    }
    
    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
    
    @Bean
    public RelationalMappingContext mappingContext() {
        return new RelationalMappingContext();
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    @Bean
    public Dialect sqlDialect(JdbcTemplate jdbcTemplate) {
        return getDialect(jdbcTemplate);
    }
    
    /**
     * Override this method to provide proper sql dialect.
     * 
     * @param jdbcTemplate not null
     * @return Implementation of {@link Dialect}
     */
    protected Dialect getDialect(JdbcTemplate jdbcTemplate) {
        return MySqlDialect.INSTANCE;
    }
    
}
