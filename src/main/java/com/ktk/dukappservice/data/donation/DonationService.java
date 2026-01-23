package com.ktk.dukappservice.data.donation;

import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DonationService extends BaseService<Donation, Long> {

    private final DonationRepository repository;

    public DonationService(DonationRepository repository) {
        this.repository = repository;
    }


    public List<Donation> fetchByQuery(String dateTime){
        if(dateTime.isEmpty()){
            return  repository.fetchByQuery(null);
        }
        LocalDateTime date = LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME);
        return  repository.fetchByQuery(date);
    }
    @Override
    protected JpaRepository<Donation, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<Donation> getEntityClass() {
        return Donation.class;
    }

    @Override
    public Donation createEntity() {
        return new Donation();
    }
}
