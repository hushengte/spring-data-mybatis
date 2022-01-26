package org.springframework.data.mybatis.statement;

import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.StatementBuilder;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

class FindAll extends AbstractStatement {

    public FindAll() {
        super(FIND_ALL, SqlCommandType.SELECT);
    }

    /**
     * Create a {@code SELECT … FROM … } statement: selecting all simple properties of an entity, 
     * including those for one-to-one relationships.
     *
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        Select select = StatementBuilder.select(tableInfo.getAliasedColumns())
                .from(tableInfo.getAliasedTable())
                .build();
        return SqlRenderer.create(renderContext).render(select);
    }

}
