package org.springframework.data.mybatis.statement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.support.SimpleMybatisRepository;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.render.RenderContext;

/**
 * Utility class to configure implementations of {@link org.springframework.data.mybatis.statement.Statement}
 * that is to be used by {@link SimpleMybatisRepository}.
 * 
 * @see org.springframework.data.mybatis.statement.Statement
 * @see org.springframework.data.mybatis.statement.AbstractStatement
 */
public class Statements {
    
    private static final Logger logger = LoggerFactory.getLogger(Statements.class);

    /**
     * Configure default {@link ResultMap}s and {@link MappedStatement}s for mybatis.
     * Normally, this method should be called before adding "other mybatis Mappers" to mybatis configuration,
     * so that "default resultMaps" added by this method can be used by resolving "other mybatis Mappers"' query methods.
     * 
     * @param config Mybatis configuration
     * @param repositoryType Subclass type of {@link MybatisRepository}
     * @param domainType Domain type for {@link MybatisRepository}
     * @param mappingContext {@link RelationalMappingContext}
     * @param dialect Implementation of {@link Dialect}
     */
    public static void configure(org.apache.ibatis.session.Configuration config, Class<?> repositoryType, Class<?> domainType, 
            RelationalMappingContext mappingContext, Dialect dialect) {
        
        String namespace = repositoryType.getName();
        configureDefaultResultMap(config, namespace, mappingContext, domainType);
        
        TableInfo tableInfo = TableInfo.create(mappingContext, domainType, config.isMapUnderscoreToCamelCase());
        RenderContext renderContext = new RenderContextFactory(dialect).createRenderContext();
        List<AbstractStatement> defaultStatements = Arrays.asList(
                new org.springframework.data.mybatis.statement.Insert(),
                new UpdateById(),
                
                new FindById(),
                new CountById(),
                new CountAll(),
                new FindAll(),
                new FindByIds(),
                new ReadLockById(),
                new WriteLockById(),
                
                new DeleteById(),
                new DeleteByIds(),
                new DeleteAll()
                );
        defaultStatements.forEach(statement -> {
            statement.configure(config, namespace, renderContext, tableInfo);
        });
    }
    
    public static void configureDefaultResultMap(org.apache.ibatis.session.Configuration config, String namespace, 
            RelationalMappingContext mappingContext, Class<?> domainType) {
        String nestedResultMapIdPrefix = Statement.DOT + Statement.RESULTMAP_DEFAULT;
        String defaultResultMapId = namespace + Statement.DOT + Statement.RESULTMAP_DEFAULT;
        buildDefaultResultMap(config, namespace, defaultResultMapId, nestedResultMapIdPrefix, mappingContext, domainType, new ArrayDeque<>());
    }
    
    private static String bracket(String name) {
        return new StringBuilder("[").append(name).append("]").toString();
    }
    
    private static ResultMap buildDefaultResultMap(org.apache.ibatis.session.Configuration config,
            String namespace, String defaultResultMapId, String nestedResultMapIdPrefix, 
            RelationalMappingContext mappingContext, Class<?> domainType, Deque<Class<?>> enclosingClassStack) {
        
        RelationalPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(domainType);
        boolean underscoreColumn = config.isMapUnderscoreToCamelCase();
        List<ResultMapping> mappings = new ArrayList<>();
        
        entity.doWithProperties((PropertyHandler<RelationalPersistentProperty>) property -> {
            Class<?> propertyType = property.getActualType();
            String propertyName = property.getName();
            String column = underscoreColumn ? TableInfo.underscoreName(propertyName) : propertyName;
            ResultMapping.Builder mappingBuilder = new ResultMapping.Builder(config, propertyName, column, propertyType);
            if (property.isEntity()) {
                if (enclosingClassStack.contains(propertyType)) {
                    logger.info("Circular entity graph detected, skipping entityType: {}", propertyType.getName());
                } else {
                    StringBuilder nestedResultMapIdPrefixBuilder = new StringBuilder(nestedResultMapIdPrefix);
                    nestedResultMapIdPrefixBuilder.append(bracket(propertyName));
                    String nestedResultMapId = namespace + nestedResultMapIdPrefixBuilder.toString();
                    enclosingClassStack.push(domainType);
                    ResultMap resultMap = buildDefaultResultMap(config, namespace, nestedResultMapId, nestedResultMapIdPrefixBuilder.toString(), 
                            mappingContext, propertyType, enclosingClassStack);
                    mappingBuilder.nestedResultMapId(resultMap.getId());
                    mappingBuilder.columnPrefix(column + "_");
                    mappings.add(mappingBuilder.build());
                }
            } else {
                if (property.isIdProperty()) {
                    mappingBuilder.flags(Arrays.asList(ResultFlag.ID));
                }
                mappings.add(mappingBuilder.build());
            }
        });
        
        Class<?> enclosingType = enclosingClassStack.pollFirst();
        String resultMapId = enclosingType == null ? defaultResultMapId : namespace + nestedResultMapIdPrefix;
        Boolean autoMapping = enclosingType == null ? true : AutoMappingBehavior.FULL.equals(config.getAutoMappingBehavior());
        
        logger.info("Building default resultMap: domainType={}, resultMapId={}", domainType.getName(), resultMapId);
        ResultMap resultMap = new ResultMap.Builder(config, resultMapId, domainType, mappings, autoMapping).build();
        config.addResultMap(resultMap);
        return resultMap;
    }
    
}
