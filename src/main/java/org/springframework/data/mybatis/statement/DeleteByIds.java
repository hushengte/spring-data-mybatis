package org.springframework.data.mybatis.statement;

import java.util.List;

import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Delete;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

class DeleteByIds extends AbstractStatement {
    
    public DeleteByIds() {
        super(DELETE_BY_IDS, SqlCommandType.DELETE);
    }

    /**
     * Create a {@code DELETE FROM … WHERE :ids in (…)} statement.
     *
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        Table table = tableInfo.getTable();
        Column idColumn = tableInfo.getIdColumn();
        Delete delete = Delete.builder()
                .from(table)
                .where(idColumn.in(forEachIdsBindMarker()))
                .build();
        return scriptTag(SqlRenderer.create(renderContext).render(delete));
    }
    
    @Override
    protected SqlSource createSqlSource(Configuration config, String sqlScript) {
        return config.getDefaultScriptingLanguageInstance().createSqlSource(config, sqlScript, List.class);
    }

}
