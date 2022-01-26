package org.springframework.data.mybatis.domain;

import org.springframework.data.relational.core.mapping.Table;

@Table("lib_publisher")
public class Publisher extends IntId {
	
    private String name;
    private String place;
    
    public Publisher() {}
    
    public Publisher(Integer id) {
        this.setId(id);
    }
    
    public Publisher(String name, String place) {
		this.name = name;
		this.place = place;
	}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	public String getPlace() {
		return place;
	}

	public void setPlace(String place) {
		this.place = place;
	}

}
