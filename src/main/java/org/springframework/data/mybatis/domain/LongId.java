package org.springframework.data.mybatis.domain;

import org.springframework.data.annotation.Id;

public abstract class LongId extends AbstractPersistable<Long> {

    @Id
    private Long id;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
}
