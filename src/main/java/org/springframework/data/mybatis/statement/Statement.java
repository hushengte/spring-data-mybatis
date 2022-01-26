package org.springframework.data.mybatis.statement;

/**
 * Marker interface for implementations that is to 
 * generate {@link org.apache.ibatis.mapping.MappedStatement}.
 * 
 * Additionally, it also used as a constant pool for convenience.
 * 
 * @see org.springframework.data.mybatis.statement.AbstractStatement
 */
public interface Statement {

    String INSERT = "insert";
    String UPDATE_BY_ID = "updateById";
    
    String COUNT_BY_ID = "countById";
    String COUNT_ALL = "countAll";
    String FIND_BY_ID = "findById";
    String FIND_BY_IDS = "findByIds";
    String FIND_ALL = "findAll";
    String READ_LOCK_BY_ID = "readLockById";
    String WRITE_LOCK_BY_ID = "writeLockById";
    
    String DELETE_BY_ID = "deleteById";
    String DELETE_BY_IDS = "deleteByIds";
    String DELETE_ALL = "deleteAll";
    
    String DOT = ".";
    String COMMA = ",";
    
    String RESULTMAP_DEFAULT = "defaultResultMap";
    
    String SCRIPT_BEGIN = "<script>";
    String SCRIPT_END = "</script>";
    
    /**
     * Render a mybatis parameter placeholder: #{paramName}
     * @param paramName parameter Name
     * @return parameter marker
     */
    default String marker(String paramName) {
        return new StringBuilder("#{").append(paramName).append("}").toString();
    }
    
    /**
     * Tag a sql as a mybatis statement script.
     * @param sqlText sql statement to tag
     * @return mybatis statement script
     */
    default String scriptTag(String sqlText) {
        return new StringBuilder(SCRIPT_BEGIN)
                .append(sqlText).append(SCRIPT_END).toString();
    }
    
    /**
     * Render a mybatis foreach script
     * @param collectionName collection name
     * @param itemName item name
     * @param separator separator of items
     * @param open beginning chars
     * @param close ending chars
     * @return foreach script
     */
    default String forEachScript(String collectionName, String itemName, String separator, 
            String open, String close) {
        StringBuilder script = new StringBuilder("<foreach collection='")
                .append(collectionName)
                .append("' item='").append(itemName)
                .append("' separator='").append(COMMA).append("'");
        if (open != null) {
            script.append(" open='").append(open).append("'");
        }
        if (close != null) {
            script.append(" close='").append(close).append("'");
        }
        script.append(">").append(marker(itemName)).append("</foreach>");
        return script.toString();
    }
    
    /**
     * Render a mybatis foreach script with a comma separator and no beginning and ending chars
     * @param collectionName collection name
     * @param itemName item name
     * @return comma separated foreach script
     */
    default String forEachCommaScript(String collectionName, String itemName) {
        return forEachScript(collectionName, itemName, COMMA, null, null);
    }
    
}
