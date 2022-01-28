package org.springframework.data.mybatis.statement.page;

import java.util.Collections;
import java.util.List;

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.mybatis.repository.query.PageableInteceptor;
import org.springframework.data.mybatis.statement.AbstractStatement;
import org.springframework.data.mybatis.statement.TableInfo;
import org.springframework.data.relational.core.sql.render.RenderContext;

/**
 * Generate "count()" statement for a sql.
 * 
 * @see PageableInteceptor
 */
public class Count extends AbstractStatement {

    private final String originalStatementId;
    private final String originalSql;
    private final List<ParameterMapping> parameterMappings;
    
    public Count(String originalStatementId, String originalSql, List<ParameterMapping> parameterMappings) {
        super(statementName(originalSql), SqlCommandType.SELECT);
        this.originalStatementId = originalStatementId;
        this.originalSql = originalSql;
        this.parameterMappings = parameterMappings;
    }

    private static String statementName(String originalSql) {
        return String.valueOf(originalSql.hashCode());
    }
    
    @Override
    protected String statementId(String namespace) {
        return new StringBuilder(originalStatementId).append("-count-").append(this.getName()).toString();
    }

    /**
     * Create a {@code SELECT COUNT(*) FROM (:originalSql)} statement.
     *
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        return new StringBuilder("SELECT COUNT(*) FROM (").append(originalSql).append(")").toString();
    }

    @Override
    protected SqlSource createSqlSource(Configuration config, String sqlScript) {
        return new StaticSqlSource(config, sqlScript, parameterMappings);
    }

    @Override
    protected void configureBuilder(Configuration config, String namespace, Builder builder) {
        builder.resultMaps(Collections.singletonList(basicTypeResultMap(config, Long.class)));
    }
    
}
