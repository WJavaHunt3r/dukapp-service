package com.ktk.dukappservice.service.notifications;

import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.JobRegistrationStatus;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Push notification texts, in Hungarian like the e-mails. */
final class NotificationTexts {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final int MAX_DESCRIPTION = 80;

    private NotificationTexts() {
    }

    record Text(String title, String body) {
    }

    static Text jobNew(Job job) {
        return new Text("Új munka", describe(job));
    }

    static Text jobCancelled(Job job) {
        return new Text("Munka lemondva", "Elmarad: " + describe(job));
    }

    static Text registeredByOther(Job job, User actor, JobRegistrationStatus status) {
        String action = status == JobRegistrationStatus.WAITLISTED ? "várólistára tett" : "jelentkeztetett";
        return new Text("Jelentkeztettek", actor.getFullName() + " " + action + ": " + describe(job));
    }

    static Text transactionsCreated(List<String> descriptions) {
        if (descriptions.size() == 1) {
            String description = descriptions.getFirst();
            return new Text("Új tranzakció", description == null || description.isBlank() ? "Új tranzakció került rögzítésre." : truncate(description));
        }
        return new Text("Új tranzakciók", descriptions.size() + " új tranzakció került rögzítésre.");
    }

    static Text chatMessage(Job job, User sender, String message) {
        String body = sender.getFullName() + ": " + message.replaceAll("\\s+", " ");
        return new Text(truncate(job.getDescription()), body.length() <= 140 ? body : body.substring(0, 139) + "…");
    }

    static Text jobNotClosed(Job job, boolean reminder) {
        return reminder
                ? new Text("Emlékeztető: lezáratlan munka", "Még mindig nincs lezárva: " + describe(job))
                : new Text("Zárd le a munkát", "Lejárt, de még nem adtad meg az órákat: " + describe(job));
    }

    static Text jobsNotClosed(List<Job> jobs) {
        if (jobs.size() == 1) {
            return new Text("Lezáratlan munka", "Még nincsenek megadva az órák: " + describe(jobs.getFirst()));
        }
        return new Text("Lezáratlan munkák", jobs.size() + " munkád vár lezárásra, add meg az órákat.");
    }

    private static String describe(Job job) {
        return truncate(job.getDescription()) + " (" + DATE.format(job.getJobDateTime()) + ")";
    }

    private static String truncate(String text) {
        return text.length() <= MAX_DESCRIPTION ? text : text.substring(0, MAX_DESCRIPTION - 1) + "…";
    }
}
