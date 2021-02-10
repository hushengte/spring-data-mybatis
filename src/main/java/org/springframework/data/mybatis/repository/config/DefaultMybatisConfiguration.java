package org.springframework.data.mybatis.repository.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mybatis.repository.query.PageableInteceptor;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;

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
    public RelationalMappingContext mappingContext() {
        return new RelationalMappingContext();
    }
    
}
