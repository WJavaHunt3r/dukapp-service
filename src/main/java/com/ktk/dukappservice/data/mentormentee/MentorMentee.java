package com.ktk.dukappservice.data.mentormentee;

import com.ktk.dukappservice.data.BaseEntity;
import com.ktk.dukappservice.data.users.User;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Entity
@Table(name = "MENTOR_MENTEE", uniqueConstraints =
        { @UniqueConstraint(name = "UniqueMentorAndMentee", columnNames = { "MENTOR", "MENTEE" })})
@FieldNameConstants
public class MentorMentee extends BaseEntity<MentorMentee, Long> {

    @NotNull
    @JoinColumn(name = "MENTOR")
    @ManyToOne
    private User mentor;

    @NotNull
    @JoinColumn(name = "MENTEE")
    @ManyToOne
    private User mentee;
}
