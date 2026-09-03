package com.ktk.dukappservice.data.church;

import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class ChurchService extends BaseService<Church, Long> {
    private final ChurchRepository churchRepository;

    public ChurchService(ChurchRepository churchRepository) {
        this.churchRepository = churchRepository;
    }

    @Override
    protected JpaRepository<Church, Long> getRepository() {
        return churchRepository;
    }

    @Override
    public Class<Church> getEntityClass() {
        return Church.class;
    }

    @Override
    public Church createEntity() {
        return new Church();
    }
}
