package org.springframework.data.mybatis.statement;

import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Delete;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

class DeleteById extends AbstractStatement {
    
    public DeleteById() {
        super(DELETE_BY_ID, SqlCommandType.DELETE);
    }

    /**
     * Create a {@code DELETE FROM … WHERE :id = …} statement.
     *
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        Table table = tableInfo.getTable();
        Column idColumn = tableInfo.getIdColumn();
        Delete delete = Delete.builder()
                .from(table)
                .where(idColumn.isEqualTo(getBindMarker(tableInfo.getIdColumnName())))
                .build();
        return SqlRenderer.create(renderContext).render(delete);
    }

}
