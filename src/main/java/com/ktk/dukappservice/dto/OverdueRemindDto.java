package com.ktk.dukappservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class OverdueRemindDto {
    /** Users to remind; empty = everyone who has unclosed jobs. */
    private Set<Long> userIds = new HashSet<>();
}
