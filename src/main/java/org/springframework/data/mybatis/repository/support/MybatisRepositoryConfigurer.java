package org.springframework.data.mybatis.repository.support;

import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.statement.Statements;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;

/**
 * Support class for filtering the mappers, the marker interface is {@link MybatisRepository}
 */
public class MybatisRepositoryConfigurer extends MapperScannerConfigurer {
    
    public MybatisRepositoryConfigurer() {
        // set to false to avoid adding mappers to mybatis configuration, 
        // we will add these mappers manually.
        setAddToConfig(false);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        super.postProcessBeanFactory(beanFactory);
        configureRepository(beanFactory, MybatisRepository.class);
    }
    
    private static Class<?> loadMapperClass(String mapperClassName) {
        try {
            return Class.forName(mapperClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
    
    private void configureRepository(ConfigurableListableBeanFactory beanFactory, Class<?> baseMapperClass) {
        org.apache.ibatis.session.Configuration configuration = beanFactory.getBean(org.apache.ibatis.session.Configuration.class);
        RelationalMappingContext mappingContext = beanFactory.getBean(RelationalMappingContext.class);
        Dialect dialect = beanFactory.getBean(Dialect.class);
        
        String[] mapperFactoryBeanNames = beanFactory.getBeanNamesForType(MapperFactoryBean.class, false, false);
        for (String mapperFactoryBeanName : mapperFactoryBeanNames) {
            String mapperBeanName = BeanFactoryUtils.transformedBeanName(mapperFactoryBeanName);
            BeanDefinition mapperFactoryBeanDef = beanFactory.getBeanDefinition(mapperBeanName);
            String mapperClassName = (String)mapperFactoryBeanDef.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
            Class<?> mapperClass = loadMapperClass(mapperClassName);
            if (mapperClass != baseMapperClass) {
                Class<?> entityType = GenericTypeResolver.resolveTypeArguments(mapperClass, baseMapperClass)[0];
                // First, Configure default resultMaps and MappedStatements
                Statements.configure(configuration, baseMapperClass, entityType, mappingContext, dialect);
                // Then, add Mapper class to configuration
                configuration.addMapper(mapperClass);
            }
        }
    }
    
}
