package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.users.User;
import org.junit.jupiter.api.Test;

import static com.ktk.dukappservice.controllers.AuthController.resolveGoogleName;
import static org.assertj.core.api.Assertions.assertThat;

class GoogleNameResolutionTest {

    @Test
    void usesGivenAndFamilyNameWhenBothAreSent() {
        assertThat(resolveGoogleName("Anna", "Kis", "Anna Kis")).isEqualTo(new AuthController.ResolvedName("Anna", "Kis"));
    }

    @Test
    void takesFamilyNameFromFullNameInEitherOrder() {
        assertThat(resolveGoogleName("Anna", null, "Anna Kis").lastname()).isEqualTo("Kis");
        assertThat(resolveGoogleName("Anna", "", "Kis Anna").lastname()).isEqualTo("Kis");
        assertThat(resolveGoogleName("Anna Mária", null, "Kis-Nagy Anna Mária"))
                .isEqualTo(new AuthController.ResolvedName("Anna Mária", "Kis-Nagy"));
    }

    @Test
    void splitsFullNameWhenGivenNameIsMissing() {
        assertThat(resolveGoogleName(null, null, "Anna Kis")).isEqualTo(new AuthController.ResolvedName("Anna", "Kis"));
    }

    @Test
    void marksMissingPartsWithPlaceholder() {
        AuthController.ResolvedName onlyGiven = resolveGoogleName("Anna", null, "Anna");
        assertThat(onlyGiven).isEqualTo(new AuthController.ResolvedName("Anna", User.NAME_PLACEHOLDER));
        assertThat(onlyGiven.complete()).isFalse();

        AuthController.ResolvedName nothing = resolveGoogleName(" ", null, null);
        assertThat(nothing).isEqualTo(new AuthController.ResolvedName(User.NAME_PLACEHOLDER, User.NAME_PLACEHOLDER));

        assertThat(resolveGoogleName(null, "Kis", null)).isEqualTo(new AuthController.ResolvedName(User.NAME_PLACEHOLDER, "Kis"));
    }

    @Test
    void userWithPlaceholderNameIsIncompleteUntilFixed() {
        User user = new User();
        user.setFirstname("Anna");
        user.setLastname(User.NAME_PLACEHOLDER);
        assertThat(user.isProfileIncomplete()).isTrue();

        user.setLastname("Kis");
        assertThat(user.isProfileIncomplete()).isFalse();
    }
}
