package org.springframework.data.mybatis.domain;

import org.springframework.data.annotation.Id;

/**
 * Abstract base class for managable entities with a long type id.
 */
public abstract class LongIdEntity extends AbstractManagable<Long> {
    
    @Id
    private Long id;
    
    private Long createBy;

    private Long updateBy;
    
    public LongIdEntity() {
        super();
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getCreateBy() {
        return createBy;
    }

    @Override
    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }
    
    @Override
    public Long getUpdateBy() {
        return updateBy;
    }

    @Override
    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

}
