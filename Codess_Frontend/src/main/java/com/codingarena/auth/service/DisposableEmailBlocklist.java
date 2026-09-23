package com.codingarena.auth.service;

import java.util.Set;

public final class DisposableEmailBlocklist {

    private static final Set<String> BLOCKED_DOMAINS = Set.of(
            "mailinator.com",
            "10minutemail.com",
            "tempmail.com",
            "guerrillamail.com",
            "throwawaymail.com",
            "yopmail.com",
            "sharklasers.com",
            "dispostable.com",
            "getairmail.com",
            "temp-mail.org",
            "fakemailgenerator.com",
            "burnermail.io",
            "trashmail.com",
            "mohmal.com",
            "nada.ltd",
            "crazymailing.com",
            "mytemp.email"
    );

    private DisposableEmailBlocklist() {
    }

    public static boolean isBlocked(String email) {
        if (email == null) {
            return false;
        }
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1 || atIndex == email.length() - 1) {
            return false;
        }
        String domain = email.substring(atIndex + 1).toLowerCase().trim();
        return BLOCKED_DOMAINS.contains(domain);
    }

    public static Set<String> getBlockedDomains() {
        return BLOCKED_DOMAINS;
    }
}
