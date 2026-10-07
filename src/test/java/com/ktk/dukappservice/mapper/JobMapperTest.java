package com.ktk.dukappservice.mapper;

import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.dto.JobDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JobMapperTest {

    private final JobMapper mapper = new JobMapper();

    private JobDto dto(String comment) {
        JobDto dto = new JobDto();
        dto.setJobDateTime(LocalDateTime.now().plusDays(1));
        dto.setDescription(" Moving boxes ");
        dto.setComment(comment);
        return dto;
    }

    @Test
    void commentIsCopiedOnCreateAndOnEdit() {
        Job job = mapper.dtoToEntity(dto("  Bring gloves\nand water  "), new Job());
        assertThat(job.getComment()).isEqualTo("Bring gloves\nand water");

        // editing the same job replaces the comment, and an empty one clears it
        mapper.dtoToEntity(dto("Updated details"), job);
        assertThat(job.getComment()).isEqualTo("Updated details");
        mapper.dtoToEntity(dto("   "), job);
        assertThat(job.getComment()).isNull();
    }
}
