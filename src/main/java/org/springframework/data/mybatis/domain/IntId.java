package org.springframework.data.mybatis.domain;

import org.springframework.data.annotation.Id;

public abstract class IntId extends AbstractPersistable<Integer> {

    @Id
    private Integer id;

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
}
