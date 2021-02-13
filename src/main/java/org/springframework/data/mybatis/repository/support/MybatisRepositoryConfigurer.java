package org.springframework.data.mybatis.repository.support;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.TextSqlNode;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

public class MybatisRepositoryConfigurer extends MapperScannerConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(MybatisRepositoryConfigurer.class);
    
    private static final boolean JPA_API_PRESENT = ClassUtils.isPresent("javax.persistence.Table", 
            MybatisRepositoryConfigurer.class.getClassLoader());
    private static final Map<Class<?>, String> REPOSITORY_CLASS_TO_TABLE_MAP = new HashMap<>();
    
    public MybatisRepositoryConfigurer() {
        // set to false to avoid adding mappers to mybatis configuration, 
        // we will add these mappers manually.
        setAddToConfig(false);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        super.postProcessBeanFactory(beanFactory);
        registerDefaultResultMapAndStatement(beanFactory, MybatisRepository.class);
    }
    
    private static Class<?> loadMapperClass(String mapperClassName) {
        try {
            return Class.forName(mapperClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
    
    private void registerDefaultResultMapAndStatement(ConfigurableListableBeanFactory beanFactory, Class<?> baseMapperClass) {
        org.apache.ibatis.session.Configuration configuration = beanFactory.getBean(org.apache.ibatis.session.Configuration.class);
        
        String[] mapperFactoryBeanNames = beanFactory.getBeanNamesForType(MapperFactoryBean.class, false, false);
        for (String mapperFactoryBeanName : mapperFactoryBeanNames) {
            String mapperBeanName = BeanFactoryUtils.transformedBeanName(mapperFactoryBeanName);
            BeanDefinition mapperFactoryBeanDef = beanFactory.getBeanDefinition(mapperBeanName);
            String mapperClassName = (String)mapperFactoryBeanDef.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
            Class<?> mapperClass = loadMapperClass(mapperClassName);
            if (mapperClass != baseMapperClass) {
                Class<?> entityType = GenericTypeResolver.resolveTypeArguments(mapperClass, baseMapperClass)[0];
                configure(configuration, mapperClass, entityType);
                configuration.addMapper(mapperClass);
            }
        }
    }
    
    /**
     * Add default resultMaps and MappedStatements to mybatis configuration.
     * Normally, this method should be called before adding "Mappers" to mybatis configuration,
     * so that "default resultMaps" registered by this method can be used by query method resolving.
     * 
     * @param configuration mybatis configuration
     * @param repositoryClass {@link MybatisRepository} subclass
     * @param entityType Domain class of Mapper
     */
    public static void configure(org.apache.ibatis.session.Configuration configuration, 
            Class<?> repositoryClass, Class<?> entityType) {
        // mapping table name
        REPOSITORY_CLASS_TO_TABLE_MAP.put(repositoryClass, resolveTableName(entityType));
        
        String namespace = repositoryClass.getName();
        // add insert and update statement
        addInsertStatement(configuration, namespace, entityType);
        addUpdateStatement(configuration, namespace, entityType);
        
        // add default resultMap for entityType
        addDefaultResultMap(configuration, namespace, entityType);
    }
    
    /**
     * Get table name for a given repository class
     * @param repositoryClass A MybatisRepository subclass
     * @return Table name
     */
    public static String getTable(Class<?> repositoryClass) {
        String table = REPOSITORY_CLASS_TO_TABLE_MAP.get(selectRepositoryClass(repositoryClass));
        if (table == null) {
            throw new IllegalArgumentException("There is no table for the repository class: " + repositoryClass.getName());
        }
        return table;
    }
    
    private static Class<?> selectRepositoryClass(Class<?> repositoryClass) {
        Class<?>[] interfaceClasses = repositoryClass.getInterfaces();
        for (Class<?> interfaceClass : interfaceClasses) {
            if (MybatisRepository.class.isAssignableFrom(interfaceClass) 
                    && interfaceClass != MybatisRepository.class) {
                return interfaceClass;
            }
        }
        return repositoryClass;
    }
    
    private static String resolveTableName(Class<?> entityType) {
        Table table = AnnotationUtils.findAnnotation(entityType, Table.class);
        if (table != null) {
            return table.value();
        }
        if (JPA_API_PRESENT) {
            javax.persistence.Table jpaTable = AnnotationUtils.findAnnotation(entityType, javax.persistence.Table.class);
            if (jpaTable != null) {
                return jpaTable.name();
            }
        }
        String message = String.format("Entity %s must be annotated with @%s or @%s", 
                entityType.getName(), Table.class.getName(), javax.persistence.Table.class.getName());
        throw new MappingException(message);
    }
    
    private static void addDefaultResultMap(org.apache.ibatis.session.Configuration config,
            String namespace, Class<?> entityType) {
        String id = namespace + "." + MybatisRepository.DEFAULT_RESULTMAP;
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
    
    private static void addInsertStatement(org.apache.ibatis.session.Configuration config,
            String namespace, Class<?> entityType) {
        String statementId = namespace + ".insert";
        String insertSql = buildInsertSql(entityType, config.isMapUnderscoreToCamelCase());
        DynamicSqlSource sqlSource = new DynamicSqlSource(config, new TextSqlNode(insertSql));
        MappedStatement.Builder builder = new MappedStatement.Builder(
                config, statementId, sqlSource, SqlCommandType.INSERT);
        builder.keyProperty("entity.id").keyGenerator(Jdbc3KeyGenerator.INSTANCE);
        MappedStatement statement = builder.build();
        if (!config.hasStatement(statement.getId(), false)) {
            config.addMappedStatement(statement);
        }
    }
    
    private static void addUpdateStatement(org.apache.ibatis.session.Configuration config,
            String namespace, Class<?> entityType) {
        String statementId = namespace + ".update";
        String updateSql = buildUpdateSql(entityType, config.isMapUnderscoreToCamelCase());
        DynamicSqlSource sqlSource = new DynamicSqlSource(config, new TextSqlNode(updateSql));
        MappedStatement.Builder builder = new MappedStatement.Builder(
                config, statementId, sqlSource, SqlCommandType.UPDATE);
        MappedStatement statement = builder.build();
        if (!config.hasStatement(statement.getId(), false)) {
            config.addMappedStatement(statement);
        }
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
    
    private static String buildInsertSql(Class<?> entityType, boolean underscoreColumn) {
        StringBuilder sql = new StringBuilder("insert into ${table} (");
        List<String> columns = new ArrayList<>();
        List<String> mappingProperties = new ArrayList<>();
        ReflectionUtils.doWithFields(entityType, field -> {
            Class<?> fieldType = field.getType();
            if (isMappingField(field, fieldType)) {
                String propertyName = field.getName();
                String column = underscoreColumn ? underscoreName(propertyName) : propertyName;
                boolean isBaseEntity = Persistable.class.isAssignableFrom(fieldType);
                columns.add(getColumn(isBaseEntity, column, underscoreColumn));
                
                StringBuilder mappingProperty = new StringBuilder("#{entity.").append(propertyName);
                if (isBaseEntity) {
                    mappingProperty.append(".id");
                }
                mappingProperty.append("}");
                mappingProperties.add(mappingProperty.toString());
            }
        });
        sql.append(StringUtils.collectionToCommaDelimitedString(columns)).append(")");
        sql.append(" values (");
        sql.append(StringUtils.collectionToCommaDelimitedString(mappingProperties)).append(")");
        return sql.toString();
    }
    
    private static String buildUpdateSql(Class<?> entityType, boolean underscoreColumn) {
        StringBuilder sql = new StringBuilder("update ${table} set ");
        List<String> updateFragments = new ArrayList<>();
        ReflectionUtils.doWithFields(entityType, field -> {
            Class<?> fieldType = field.getType();
            if (isMappingField(field, fieldType)) {
                String propertyName = field.getName();
                String column = underscoreColumn ? underscoreName(propertyName) : propertyName;
                boolean isBaseEntity = Persistable.class.isAssignableFrom(fieldType);
                
                StringBuilder fragment = new StringBuilder(getColumn(isBaseEntity, column, underscoreColumn));
                fragment.append(" = #{entity.").append(propertyName);
                if (isBaseEntity) {
                    fragment.append(".id");
                }
                fragment.append("}");
                updateFragments.add(fragment.toString());
            }
        });
        sql.append(StringUtils.collectionToCommaDelimitedString(updateFragments));
        sql.append(" where `id` = #{entity.id}");
        return sql.toString();
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
    
    private static String getColumn(boolean isBaseEntity, String column, boolean underscoreColumn) {
        StringBuilder columnBuf = new StringBuilder("`").append(column);
        if (isBaseEntity) {
            columnBuf.append(underscoreColumn ? "_id" : "Id");
        }
        return columnBuf.append("`").toString();
    }
    
}
