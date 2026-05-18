package com.ktk.dukappservice.data.church;

import com.ktk.dukappservice.data.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@Entity
@Table(name = "CHURHES")
@FieldNameConstants
public class Church extends BaseEntity<Church, Long> {


    @Size(max = 50)
    @Column(name = "CHURCH_NAME", length = 50)
    @NotNull
    @NotEmpty
    private String churchName;

    @Size(max = 50)
    @Column(name = "COUNTRY", length = 50)
    @NotNull
    @NotEmpty
    private String country;
}
