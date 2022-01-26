package org.springframework.data.mybatis.statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.relational.core.mapping.PersistentPropertyPathExtension;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;

public class TableInfo {
    
    public static final String DEFAULT_TABLE_ALIAS = "t";
    
    private final Table table;
    private final Table aliasedTable;
    private SqlIdentifier idColumnName;
    private final List<SqlIdentifier> columnNames = new ArrayList<>();
    private final List<SqlIdentifier> nonIdColumnNames = new ArrayList<>();
    private final Set<SqlIdentifier> readOnlyColumnNames = new HashSet<>();
    private Set<SqlIdentifier> insertableColumns;
    private Set<SqlIdentifier> updateableColumns;
    
    public TableInfo(SqlIdentifier tableName) {
        this.table = Table.create(tableName);
        this.aliasedTable = this.table.as(DEFAULT_TABLE_ALIAS);
    }
    
    public static TableInfo create(MappingContext<RelationalPersistentEntity<?>, RelationalPersistentProperty> mappingContext,
            Class<?> domainType) {
        
        RelationalPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(domainType);
        TableInfo info = new TableInfo(entity.getTableName());
        entity.doWithProperties((PropertyHandler<RelationalPersistentProperty>) property -> {
            // the referencing column of referenced entity is expected to be on the other side of the relation
            if (!property.isEntity()) {
                initSimpleColumnName(info, property, "");
            } else if (property.isEntity()) {
                PersistentPropertyPathExtension extPath = new PersistentPropertyPathExtension(mappingContext, 
                        mappingContext.getPersistentEntity(property.getActualType()));
                if (extPath.hasIdProperty()) {
                    String propertyPrefix = property.getName() + "_";
                    SqlIdentifier columnName = extPath.getIdColumnName().transform(propertyPrefix::concat);
                    info.nonIdColumnNames.add(columnName);
                }
            }
        });
        
        info.columnNames.add(info.idColumnName);
        info.columnNames.addAll(info.nonIdColumnNames);
        
        Set<SqlIdentifier> insertable = new LinkedHashSet<>(info.nonIdColumnNames);
        insertable.removeAll(info.readOnlyColumnNames);
        info.insertableColumns = Collections.unmodifiableSet(insertable);
        
        Set<SqlIdentifier> updateable = new LinkedHashSet<>(info.columnNames);
        updateable.removeAll(info.readOnlyColumnNames);
        info.updateableColumns = Collections.unmodifiableSet(updateable);
        return info;
    }

    private static void initSimpleColumnName(TableInfo tableInfo, RelationalPersistentProperty property, String prefix) {
        SqlIdentifier columnName = property.getColumnName().transform(prefix::concat);
        
        if (property.getOwner().isIdProperty(property)) {
            tableInfo.idColumnName = columnName;
        } else {
            tableInfo.nonIdColumnNames.add(columnName);
        }
        if (!property.isWritable()) {
            tableInfo.readOnlyColumnNames.add(columnName);
        }
    }

    public Table getTable() {
        return table;
    }

    public Table getAliasedTable() {
        return aliasedTable;
    }
    
    public SqlIdentifier getIdColumnName() {
        return idColumnName;
    }
    
    public Column getIdColumn() {
        return getTable().column(idColumnName);
    }

    public List<SqlIdentifier> getColumnNames() {
        return columnNames;
    }

    public List<Column> getColumns() {
        return columnNames.stream()
                .map(columnName -> table.column(columnName))
                .collect(Collectors.toList());
    }
    
    public List<SqlIdentifier> getNonIdColumnNames() {
        return nonIdColumnNames;
    }

    public Set<SqlIdentifier> getReadOnlyColumnNames() {
        return readOnlyColumnNames;
    }

    public Set<SqlIdentifier> getInsertableColumns() {
        return insertableColumns;
    }

    public Set<SqlIdentifier> getUpdateableColumns() {
        return updateableColumns;
    }
    
}
