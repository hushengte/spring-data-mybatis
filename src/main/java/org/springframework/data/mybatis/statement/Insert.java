package org.springframework.data.mybatis.statement;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.relational.core.sql.BindMarker;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

class Insert extends AbstractStatement {
    
    private static final String KEY_ID = "id";
    
    private final boolean withIdentifier;

    public Insert(boolean withIdentifier) {
        super(withIdentifier ? INSERT_WITH_ID : INSERT, SqlCommandType.INSERT);
        this.withIdentifier = withIdentifier;
    }

    /**
     * Create a {@code INSERT INTO … (…) VALUES(…)} statement.
     *
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        Table table = tableInfo.getTable();
        Set<SqlIdentifier> insertColumns = buildInsertColumns(tableInfo);
        List<Column> columns = insertColumns.stream()
                .map(columnName -> {
                    return table.column(columnName);
                }).collect(Collectors.toList());
        List<BindMarker> markers = insertColumns.stream()
                .map(columnName -> {
                    String mappedPropertyName = tableInfo.getMappedPropertyName(columnName);
                    return SQL.bindMarker(Statement.marker(mappedPropertyName));
                }).collect(Collectors.toList());
        return SqlRenderer.create(renderContext)
                .render(org.springframework.data.relational.core.sql.Insert.builder()
                .into(table)
                .columns(columns)
                .values(markers)
                .build());
    }
    
    private Set<SqlIdentifier> buildInsertColumns(TableInfo tableInfo) {
    	if (withIdentifier) {
    		Set<SqlIdentifier> columns = new LinkedHashSet<>();
    		columns.add(tableInfo.getIdColumnName());
    		columns.addAll(tableInfo.getInsertableColumns());
    		return columns;
        }
    	return tableInfo.getInsertableColumns();
    }
    
    @Override
    protected void configureBuilder(Configuration config, String namespace, MappedStatement.Builder builder) {
        builder.keyProperty(KEY_ID)
        	.keyGenerator(withIdentifier ? NoKeyGenerator.INSTANCE : Jdbc3KeyGenerator.INSTANCE);
    }

}
