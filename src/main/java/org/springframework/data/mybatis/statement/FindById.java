package org.springframework.data.mybatis.statement;

import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.StatementBuilder;
import org.springframework.data.relational.core.sql.Table;
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
        Table table = tableInfo.getAliasedTable();
        Column idColumn = table.column(tableInfo.getIdColumnName());
        Select select = StatementBuilder.select(tableInfo.getAliasedColumns())
                .from(table)
                .where(idColumn.isEqualTo(getBindMarker(tableInfo.getIdColumnName())))
                .build();
        return SqlRenderer.create(renderContext).render(select);
    }

}
