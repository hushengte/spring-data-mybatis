package org.springframework.data.mybatis.statement;

import java.util.List;

import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.StatementBuilder;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

class FindByIds extends AbstractStatement {

    public FindByIds() {
        super(FIND_BY_IDS, SqlCommandType.SELECT);
    }
    
    /**
     * Returns a query for selecting all simple properties of an entity, including those for one-to-one relationships.
     * Results are filtered using an {@code IN}-clause on the id column.
     *
     * @return a SQL statement. Guaranteed to be not {@code null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        Select select = StatementBuilder.select(tableInfo.getColumns())
                .from(tableInfo.getTable())
                .where(tableInfo.getIdColumn().in(forEachIdsBindMarker()))
                .build();
        return scriptTag(SqlRenderer.create(renderContext).render(select));
    }

    @Override
    protected SqlSource createSqlSource(Configuration config, String sqlScript) {
        return config.getDefaultScriptingLanguageInstance().createSqlSource(config, sqlScript, List.class);
    }

}
