package org.springframework.data.mybatis.statement;

import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.data.relational.core.sql.Delete;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

class DeleteAll extends AbstractStatement {
    
    public DeleteAll() {
        super(DELETE_ALL, SqlCommandType.DELETE);
    }

    /**
     * Create a {@code DELETE FROM … } statement.
     *
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        Delete delete = Delete.builder()
                .from(tableInfo.getTable())
                .build();
        return SqlRenderer.create(renderContext).render(delete);
    }

}
