package org.springframework.data.mybatis.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Abstract base class for managable entities.
 *
 * @param <ID> the type of the managing type's identifier.
 */
public abstract class AbstractManagable<ID extends Serializable> extends AbstractPersistable<ID> implements 
        Managable<ID, ID, LocalDateTime> {
    
    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    private Date createTime;
    
    private Date updateTime;
    
    private Integer deleted;
    
    public AbstractManagable() {
        this.createTime = new Date();
        this.updateTime = this.createTime;
        this.deleted = NOT_DELETED;
    }

    @Override
    public LocalDateTime getCreateTime() {
        return createTime != null ? 
                LocalDateTime.ofInstant(createTime.toInstant(), ZoneId.systemDefault()) : null;
    }

    @Override
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = Date.from(createTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public LocalDateTime getUpdateTime() {
        return updateTime != null ? 
                LocalDateTime.ofInstant(updateTime.toInstant(), ZoneId.systemDefault()) : null;
    }

    @Override
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = Date.from(updateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public boolean deleted() {
        return deleted != null ? deleted.equals(DELETED) : false;
    }
    
}
