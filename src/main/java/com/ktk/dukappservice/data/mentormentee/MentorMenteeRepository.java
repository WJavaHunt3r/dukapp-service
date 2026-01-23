package com.ktk.dukappservice.data.mentormentee;

import com.ktk.dukappservice.data.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MentorMenteeRepository extends JpaRepository<MentorMentee, Long> {

    List<MentorMentee> findByMentor(User mentor);
}
