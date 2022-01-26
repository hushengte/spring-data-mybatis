package org.springframework.data.mybatis.statement;

import java.util.Collections;

import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.StatementBuilder;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

class WriteLockById extends AbstractStatement {

    public WriteLockById() {
        super(WRITE_LOCK_BY_ID, SqlCommandType.SELECT);
    }

    /**
     * Create a {@code SELECT id FROM … WHERE :id = … (LOCK CLAUSE)} statement.
     *
     * @param lockMode Lock clause mode.
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        Column idColumn = tableInfo.getIdColumn();
        Select select = StatementBuilder
            .select(idColumn)
            .from(tableInfo.getTable())
            .where(idColumn.isEqualTo(getBindMarker(tableInfo.getIdColumnName())))
            .lock(LockMode.PESSIMISTIC_WRITE)
            .build();
        return SqlRenderer.create(renderContext).render(select);
    }
    
    @Override
    protected void configureBuilder(Configuration config, String namespace, Builder builder) {
        builder.resultMaps(Collections.singletonList(basicTypeResultMap(config, Long.class)));
    }

}
