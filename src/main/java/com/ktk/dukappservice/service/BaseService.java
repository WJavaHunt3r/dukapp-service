package com.ktk.dukappservice.service;

import com.ktk.dukappservice.data.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Optional;

@Service
abstract public class BaseService<E extends BaseEntity<E, L>, L extends Serializable> {

    public E save(E entity) {
        return getRepository().save(entity);
    }

    public Iterable<E> saveAll(Iterable<E> entities) {
        return getRepository().saveAll(entities);
    }

    public void delete(E entity) {
        getRepository().delete(entity);
    }

    public void deleteById(L id) {
        getRepository().deleteById(id);
    }

    public void deleteAll(Iterable<E> entities) {
        getRepository().deleteAll(entities);
    }

    public void deleteAll() {
        getRepository().deleteAll();
    }

    public boolean existsById(L id) {
        return getRepository().existsById(id);
    }

    public long count() {
        return getRepository().count();
    }

    public void getById(L id) {
        getRepository().getById(id);
    }

    public Optional<E> findById(L id) {
        return getRepository().findById(id);
    }

    public Iterable<E> fetchAll() {
        return getRepository().findAll();
    }

    public Iterable<E> findAll() {
        return getRepository().findAll();
    }

    protected abstract JpaRepository<E, L> getRepository();

    public abstract Class<E> getEntityClass();

    public abstract E createEntity();
}
