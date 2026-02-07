package com.ktk.dukappservice.data;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;

@MappedSuperclass
public abstract class BaseEntity<T, I extends Serializable> implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    protected I id;

    public I getId() {
        return id;
    }

    public void setId(I id) {
        this.id = id;
    }

}

