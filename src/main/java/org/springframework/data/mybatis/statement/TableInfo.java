package org.springframework.data.mybatis.statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.util.StringUtils;

public class TableInfo {
    
    public static final String DEFAULT_TABLE_ALIAS = "t";
    
    private final Table table;
    private final Table aliasedTable;
    private SqlIdentifier idColumnName;
    private final List<SqlIdentifier> columnNames = new ArrayList<>();
    private final Map<SqlIdentifier, String> columnNamesToPropertyNamesMap = new HashMap<>();
    private final List<SqlIdentifier> nonIdColumnNames = new ArrayList<>();
    private final Set<SqlIdentifier> readOnlyColumnNames = new HashSet<>();
    private Set<SqlIdentifier> insertableColumns;
    private Set<SqlIdentifier> updateableColumns;
    
    public TableInfo(SqlIdentifier tableName) {
        this.table = Table.create(tableName);
        this.aliasedTable = this.table.as(DEFAULT_TABLE_ALIAS);
    }
    
    /**
     * Transform a "camelcase" name as "underscore" name
     * @param name the original name
     * @return underscored name
     */
    public static String underscoreName(String name) {
        if (!StringUtils.hasLength(name)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(lowerCaseName(name.substring(0, 1)));
        for (int i = 1; i < name.length(); i++) {
            String s = name.substring(i, i + 1);
            String slc = lowerCaseName(s);
            if (!s.equals(slc)) {
                result.append("_").append(slc);
            }
            else {
                result.append(s);
            }
        }
        return result.toString();
    }
    
    private static String lowerCaseName(String name) {
        return name.toLowerCase(Locale.US);
    }
    
    public static TableInfo create(MappingContext<RelationalPersistentEntity<?>, RelationalPersistentProperty> mappingContext,
            Class<?> domainType, boolean underscoreColumn) {
        
        RelationalPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(domainType);
        TableInfo info = new TableInfo(entity.getTableName());
        entity.doWithProperties((PropertyHandler<RelationalPersistentProperty>) property -> {
            if (property.isEntity()) {
                RelationalPersistentEntity<?> propertyEntity = mappingContext.getPersistentEntity(property.getActualType());
                RelationalPersistentProperty propertyEntityId = propertyEntity.getIdProperty();
                if (propertyEntityId != null) {
                    mappingProperty(info, propertyEntityId, property.getName(), underscoreColumn);
                }
            } else {
                mappingProperty(info, property, null, underscoreColumn);
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

    private static void mappingProperty(TableInfo tableInfo, RelationalPersistentProperty property, 
            String ownerPropertyName, boolean underscoreColumn) {
        SqlIdentifier columnName = property.getColumnName();
        String mappedPropertyName = property.getName();
        if (property.getOwner().isIdProperty(property)) {
            if (StringUtils.hasText(ownerPropertyName)) {
                if (underscoreColumn) {
                    String columnPrefix = underscoreName(ownerPropertyName) + "_";
                    columnName = columnName.transform(columnPrefix::concat);
                } else {
                    columnName.transform(StringUtils::capitalize).transform(ownerPropertyName::concat);
                }
                tableInfo.nonIdColumnNames.add(columnName);
                mappedPropertyName = ownerPropertyName + "." + mappedPropertyName;
            } else {
                tableInfo.idColumnName = columnName;
            }
        } else {
            tableInfo.nonIdColumnNames.add(columnName);
        }
        if (!property.isWritable()) {
            tableInfo.readOnlyColumnNames.add(columnName);
        }
        tableInfo.columnNamesToPropertyNamesMap.put(columnName, mappedPropertyName);
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
    
    public List<Column> getAliasedColumns() {
        return columnNames.stream()
                .map(columnName -> aliasedTable.column(columnName))
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
    
    public String getMappedPropertyName(SqlIdentifier columnName) {
        return columnNamesToPropertyNamesMap.get(columnName);
    }
    
}
