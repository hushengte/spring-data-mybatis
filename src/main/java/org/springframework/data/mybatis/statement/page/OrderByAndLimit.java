package org.springframework.data.mybatis.statement.page;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mybatis.repository.query.PageableInteceptor;
import org.springframework.data.mybatis.statement.AbstractStatement;
import org.springframework.data.mybatis.statement.TableInfo;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.util.StringUtils;

/**
 * Generate "order by" and "limit" cause for a sql.
 * 
 * @see PageableInteceptor
 */
public class OrderByAndLimit extends AbstractStatement {
    
    private static final String PARAM_KEY_OFFSET = "offset";
    private static final String PARAM_KEY_SIZE = "size";
    
    private final String originalStatementId;
    private final String originalSql;
    private final Map<String, Object> originalParameters;
    private final List<ParameterMapping> parameterMappings;
    private final List<ResultMap> resultMaps;
    private final Pageable pageable;
    
    public OrderByAndLimit(String originalStatementId, String originalSql, Map<String, Object> originalParameters, 
            List<ParameterMapping> parameterMappings, List<ResultMap> resultMaps,
            Pageable pageable) {
        super(statementName(originalSql, pageable), SqlCommandType.SELECT);
        this.originalStatementId = originalStatementId;
        this.originalSql = originalSql;
        this.originalParameters = originalParameters;
        this.parameterMappings = parameterMappings;
        this.resultMaps = resultMaps;
        this.pageable = pageable;
    }
    
    private static String statementName(String originalSql, Pageable pageable) {
        return String.valueOf(originalSql.hashCode()) + "-" + String.valueOf(pageable.hashCode());
    }

    @Override
    protected String statementId(String namespace) {
        return new StringBuilder(originalStatementId).append("-pageable-").append(this.getName()).toString();
    }

    /**
     * Create a {@code :originalSql [ORDER BY ...] LIMIT ?, ?} statement.
     *
     * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
     */
    @Override
    public String renderSql(RenderContext renderContext, TableInfo tableInfo) {
        StringBuilder sqlBuf = new StringBuilder(originalSql);
        String sortFragment = buildOrderByFragment(pageable.getSort());
        if (sortFragment.length() > 0) {
            sqlBuf.append(" ORDER BY ").append(sortFragment);
        }
        sqlBuf.append(" LIMIT ?,?");
        return sqlBuf.toString();
    }

    /**
     * Construct "order by" sql fragment for {@link Sort}
     * @param sort Sort object
     * @return "order by" sql fragment
     */
    public static String buildOrderByFragment(Sort sort) {
        if (sort != null && sort.isSorted()) {
            Iterator<Order> orderIter = sort.iterator();
            List<String> sorts = new ArrayList<>();
            while (orderIter.hasNext()) {
                Order order = orderIter.next();
                StringBuilder sortBuf = new StringBuilder();
                sortBuf.append(order.getProperty()).append(" ").append(order.getDirection().name());
                sorts.add(sortBuf.toString());
            }
            return StringUtils.collectionToCommaDelimitedString(sorts);
        }
        return "";
    }
    
    @Override
    protected SqlSource createSqlSource(Configuration config, String sqlScript) {
        originalParameters.put(PARAM_KEY_OFFSET, (int)pageable.getOffset());
        originalParameters.put(PARAM_KEY_SIZE, pageable.getPageSize());
        
        List<ParameterMapping> finalParameterMappings = new ArrayList<>();
        finalParameterMappings.addAll(this.parameterMappings);
        finalParameterMappings.add(new ParameterMapping.Builder(config, PARAM_KEY_OFFSET, int.class).build());
        finalParameterMappings.add(new ParameterMapping.Builder(config, PARAM_KEY_SIZE, int.class).build());
        return new StaticSqlSource(config, sqlScript, finalParameterMappings);
    }

    @Override
    protected void configureBuilder(Configuration config, String namespace, Builder builder) {
        builder.resultMaps(resultMaps);
    }
    
}
