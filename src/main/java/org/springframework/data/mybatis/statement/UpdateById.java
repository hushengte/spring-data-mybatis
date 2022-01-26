package org.springframework.data.mybatis.statement;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.data.relational.core.sql.AssignValue;
import org.springframework.data.relational.core.sql.Assignments;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.Update;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

class UpdateById extends AbstractStatement {
    
    public UpdateById() {
        super(UPDATE_BY_ID, SqlCommandType.UPDATE);
    }

    /**
     * Create a {@code UPDATE … SET … WHERE :id = ...} statement.
     *
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        Table table = tableInfo.getAliasedTable();
        Column idColumn = table.column(tableInfo.getIdColumnName());
        List<AssignValue> assignments = tableInfo.getUpdateableColumns().stream()
                .map(columnName -> {
                    return Assignments.value(table.column(columnName), getBindMarker(columnName));
                }).collect(Collectors.toList());
        Update update = Update.builder()
                .table(table)
                .set(assignments)
                .where(idColumn.isEqualTo(getBindMarker(tableInfo.getIdColumnName())))
                .build();
        return SqlRenderer.create(renderContext).render(update);
    }

}
