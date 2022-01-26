package org.springframework.data.mybatis.statement;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.relational.core.sql.BindMarker;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

class Insert extends AbstractStatement {
    
    private static final String KEY_ID = "id";

    public Insert() {
        super(INSERT, SqlCommandType.INSERT);
    }

    /**
     * Create a {@code INSERT INTO … (…) VALUES(…)} statement.
     *
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        Table table = tableInfo.getTable();
        Set<SqlIdentifier> insertableColumns = tableInfo.getInsertableColumns();
        List<Column> columns = insertableColumns.stream()
                .map(columnName -> {
                    return table.column(columnName);
                }).collect(Collectors.toList());
        List<BindMarker> markers = insertableColumns.stream()
                .map(columnName -> {
                    return getBindMarker(columnName);
                }).collect(Collectors.toList());
        return SqlRenderer.create(renderContext)
                .render(org.springframework.data.relational.core.sql.Insert.builder()
                .into(table)
                .columns(columns)
                .values(markers)
                .build());
    }
    
    @Override
    protected void configureBuilder(Configuration config, String namespace, Builder builder) {
        builder.keyProperty(KEY_ID).keyGenerator(Jdbc3KeyGenerator.INSTANCE);
    }

}
