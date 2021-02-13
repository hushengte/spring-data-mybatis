package org.springframework.data.mybatis.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mybatis.dao.BookDao;
import org.springframework.data.mybatis.repository.config.DefaultMybatisConfiguration;
import org.springframework.data.mybatis.repository.config.EnableMybatisRepositories;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
@Import(DataSourceConfig.class)
@EnableMybatisRepositories(basePackageClasses = {BookDao.class})
public class MybatisConfig extends DefaultMybatisConfiguration {

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
    
}
