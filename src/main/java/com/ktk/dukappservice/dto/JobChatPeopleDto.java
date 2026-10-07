package com.ktk.dukappservice.dto;

import java.util.List;

/** Everyone in the chat of a job; {@code canManage} = the current user may add and remove chat-only members. */
public record JobChatPeopleDto(List<Person> participants, boolean canManage) {

    /** {@code role}: REGISTERED, RESPONSIBLE, CREATOR or MEMBER (added to the chat only; the only removable ones). */
    public record Person(Long userId, String userName, String role, boolean removable) {
    }
}
