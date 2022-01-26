package org.springframework.data.mybatis.statement;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.support.SimpleMybatisRepository;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

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
        configureDefaultResultMap(config, namespace, domainType);
        
        TableInfo tableInfo = TableInfo.create(mappingContext, domainType);
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
            MappedStatement ms = statement.create(config, namespace, renderContext, tableInfo);
            if (!config.hasStatement(ms.getId(), false)) {
                config.addMappedStatement(ms);
            }
        });
    }
    
    public static void configureDefaultResultMap(org.apache.ibatis.session.Configuration config,
            String namespace, Class<?> entityType) {
        String id = namespace + Statement.DOT + Statement.RESULTMAP_DEFAULT;
        String nestedResultMapIdPrefix = ".mapper_resultMap[default]";
        buildResultMap(config, namespace, id, nestedResultMapIdPrefix, entityType, new ArrayDeque<>());
    }
    
    private static ResultMap buildResultMap(org.apache.ibatis.session.Configuration config,
            String namespace, String id, String nestedResultMapIdPrefix, 
            Class<?> entityType, Deque<Class<?>> enclosingClassStack) {
        boolean underscoreColumn = config.isMapUnderscoreToCamelCase();
        List<ResultMapping> mappings = new ArrayList<>();
        ReflectionUtils.doWithFields(entityType, field -> {
            Class<?> fieldType = field.getType();
            if (isMappingField(field, fieldType)) {
                String propertyName = field.getName();
                boolean isBaseEntity = Persistable.class.isAssignableFrom(fieldType);
                String column = underscoreColumn ? underscoreName(propertyName) : propertyName;
                ResultMapping.Builder mappingBuilder = new ResultMapping.Builder(config, 
                        propertyName, column, fieldType);
                if (isBaseEntity) {
                    if (enclosingClassStack.contains(fieldType)) {
                        logger.info("Circular entity graph detected, skipping entityType: {}", fieldType.getName());
                    } else {
                        StringBuilder idPrefix = new StringBuilder(nestedResultMapIdPrefix);
                        idPrefix.append("_association[").append(propertyName).append("]");
                        String nestedResultMapId = namespace + idPrefix.toString();
                        enclosingClassStack.push(entityType);
                        ResultMap resultMap = buildResultMap(config, namespace, nestedResultMapId, idPrefix.toString(), 
                                fieldType, enclosingClassStack);
                        mappingBuilder.nestedResultMapId(resultMap.getId());
                        mappingBuilder.columnPrefix(column + "_");
                        mappings.add(mappingBuilder.build());
                    }
                } else {
                    if ("id".equals(propertyName)) {
                        mappingBuilder.flags(Arrays.asList(ResultFlag.ID));
                    }
                    mappings.add(mappingBuilder.build());
                }
            }
        });
        Class<?> enclosingType = enclosingClassStack.pollFirst();
        String resultMapId = enclosingType == null ? id : namespace + nestedResultMapIdPrefix;
        Boolean autoMapping = enclosingType == null ? true : AutoMappingBehavior.FULL.equals(config.getAutoMappingBehavior());
        ResultMap resultMap = new ResultMap.Builder(config, resultMapId, entityType, mappings, autoMapping).build();
        config.addResultMap(resultMap);
        return resultMap;
    }
    
    private static boolean isMappingField(Field field, Class<?> fieldType) {
        if (Modifier.isStatic(field.getModifiers())) {
            return false;
        }
        if (Collection.class.isAssignableFrom(fieldType)) {
            return false;
        }
        if (Map.class.isAssignableFrom(fieldType)) {
            return false;
        }
        return true;
    }
    
    private static String underscoreName(String name) {
        if (!StringUtils.hasLength(name)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(lowerCaseName(name.substring(0, 1)));
        for (int i = 1; i < name.length(); i++) {
            String s = name.substring(i, i + 1);
            String slc = lowerCaseName(s);
            if (!s.equals(slc)) {
                result.append("_").append(slc);
            }
            else {
                result.append(s);
            }
        }
        return result.toString();
    }
    
    private static String lowerCaseName(String name) {
        return name.toLowerCase(Locale.US);
    }
    
}
