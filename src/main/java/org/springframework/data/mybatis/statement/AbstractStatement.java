package org.springframework.data.mybatis.statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.TextSqlNode;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.relational.core.sql.BindMarker;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.util.Assert;

/**
 * Base class for generate a {@link MappedStatement}
 */
public abstract class AbstractStatement implements Statement {
    
    private static final Logger logger = LoggerFactory.getLogger(AbstractStatement.class);
    
    private static final Pattern NON_VISIBLE_CHAR_PATTERN = Pattern.compile("\\W");
    
    private final String name;
    private final SqlCommandType type;
    
    protected AbstractStatement(String name, SqlCommandType type) {
        Assert.notNull(name, "Statement name is required.");
        Assert.notNull(type, "Statement type is required.");
        this.name = name;
        this.type = type;
    }
    
    /**
     * Get the name of the statement
     * @return the name of the statement
     */
    public String getName() {
        return name;
    }

    /**
     * Get the type of the statement
     * @return the type of the statement
     */
    public SqlCommandType getType() {
        return type;
    }
    
    /**
     * Create a {@link MappedStatement}.
     *
     * @return a {@link MappedStatement} for mybatis.
     */
    public MappedStatement create(Configuration config, String namespace, RenderContext renderContext, TableInfo tableInfo) {
        String id = statementId(namespace);
        String sqlScript = renderSql(renderContext, tableInfo);
        if (logger.isDebugEnabled()) {
            logger.debug("Rendered SQL: {}", sqlScript);
        }
        SqlSource sqlSource = createSqlSource(config, sqlScript);
        MappedStatement.Builder builder = new MappedStatement.Builder(config, id, sqlSource, this.getType());
        configureBuilder(config, namespace, builder);
        return builder.build();
    }
    
    protected String statementId(String namespace) {
        return new StringBuilder(namespace).append(DOT).append(this.getName()).toString();
    }
    
    /**
     * Create a sql statement.
     *
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    public abstract String renderSql(RenderContext renderContext, TableInfo tableInfo);
    
    protected SqlSource createSqlSource(Configuration config, String sqlScript) {
        return new DynamicSqlSource(config, new TextSqlNode(sqlScript));
    }
    
    protected void configureBuilder(Configuration config, String namespace, MappedStatement.Builder builder) {
        if (SqlCommandType.SELECT.equals(this.getType())) {
            String resultMapId = namespace + DOT + RESULTMAP_DEFAULT;
            ResultMap resultMap = config.getResultMap(resultMapId);
            if (resultMap != null) {
                builder.resultMaps(Collections.singletonList(config.getResultMap(resultMapId)));
            }
        }
    }
    
    protected BindMarker getBindMarker(SqlIdentifier columnName) {
        String referenceName = columnName.getReference();
        String paramName = NON_VISIBLE_CHAR_PATTERN.matcher(referenceName).replaceAll("");
        return SQL.bindMarker(marker(paramName));
    }
    
    protected BindMarker forEachIdsBindMarker() {
        return SQL.bindMarker(forEachCommaScript("list", "id"));
    }

    public static ResultMap basicTypeResultMap(Configuration config, Class<?> basicType) {
        String resultMapId = basicType.getSimpleName().toLowerCase();
        return new ResultMap.Builder(config, resultMapId, basicType, new ArrayList<>()).build();
    }
    
}
