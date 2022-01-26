package org.springframework.data.mybatis.domain;

import org.springframework.data.annotation.Id;

/**
 * Abstract base class for managable entities with a int type id.
 */
public abstract class IntIdEntity extends AbstractManagable<Integer> {
    
    @Id
    private Integer id;
    
    private Integer createBy;

    private Integer updateBy;
    
    public IntIdEntity() {
        super();
    }

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getCreateBy() {
        return createBy;
    }

    @Override
    public void setCreateBy(Integer createBy) {
        this.createBy = createBy;
    }
    
    @Override
    public Integer getUpdateBy() {
        return updateBy;
    }

    @Override
    public void setUpdateBy(Integer updateBy) {
        this.updateBy = updateBy;
    }

}
