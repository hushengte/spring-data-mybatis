package org.springframework.data.mybatis.statement;

import java.util.Collections;

import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.relational.core.sql.Expressions;
import org.springframework.data.relational.core.sql.Functions;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.StatementBuilder;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

class CountAll extends AbstractStatement {

    public CountAll() {
        super(COUNT_ALL, SqlCommandType.SELECT);
    }

    /**
     * Create a {@code SELECT COUNT(*) FROM …} statement.
     *
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        Select select = StatementBuilder
                .select(Functions.count(Expressions.asterisk()))
                .from(tableInfo.getAliasedTable())
                .build();
        return SqlRenderer.create(renderContext).render(select);
    }
    
    @Override
    protected void configureBuilder(Configuration config, String namespace, Builder builder) {
        builder.resultMaps(Collections.singletonList(basicTypeResultMap(config, Long.class)));
    }

}
