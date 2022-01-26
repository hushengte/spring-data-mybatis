package org.springframework.data.mybatis.statement;

import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.StatementBuilder;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

class FindById extends AbstractStatement {

    public FindById() {
        super(FIND_BY_ID, SqlCommandType.SELECT);
    }

    /**
     * Create a {@code SELECT … FROM … WHERE :id = …} statement.
     *
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        Select select = StatementBuilder.select(tableInfo.getColumns())
                .from(tableInfo.getTable())
                .where(tableInfo.getIdColumn().isEqualTo(getBindMarker(tableInfo.getIdColumnName())))
                .build();
        return SqlRenderer.create(renderContext).render(select);
    }

}
