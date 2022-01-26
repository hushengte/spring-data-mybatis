package org.springframework.data.mybatis.repository.query;

import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

/**
 * Base class for queries based on a repository method.
 */
public abstract class AbstractMybatisQuery implements RepositoryQuery {

    private final QueryMethod queryMethod;
    
    /**
     * Creates a new {@link AbstractMybatisQuery}
     *
     * @param queryMethod must not be {@literal null}.
     */
    protected AbstractMybatisQuery(QueryMethod queryMethod) {
        Assert.notNull(queryMethod, "Query method must not be null!");
        this.queryMethod = queryMethod;
    }
    
    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.RepositoryQuery#getQueryMethod()
     */
    @Override
    public QueryMethod getQueryMethod() {
        return queryMethod;
    }
    
}
