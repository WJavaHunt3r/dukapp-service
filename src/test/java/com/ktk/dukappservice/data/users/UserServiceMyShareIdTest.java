package com.ktk.dukappservice.data.users;

import com.ktk.dukappservice.data.church.ChurchService;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceMyShareIdTest {

    private final UserRepository repository = mock(UserRepository.class);
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, mock(TransactionItemService.class), mock(ChurchService.class));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.findByMyShareID(anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void theFirstRegisteredUserGets100001() {
        when(repository.findMaxMyShareIdBetween(100001L, 999999L)).thenReturn(null);

        assertThat(service.saveNewUser(new User()).getMyShareID()).isEqualTo(100001L);
    }

    @Test
    void laterUsersGetTheNextNumberAndTakenOnesAreSkipped() {
        when(repository.findMaxMyShareIdBetween(100001L, 999999L)).thenReturn(100041L);
        when(repository.findByMyShareID(100042L)).thenReturn(Optional.of(new User()));

        assertThat(service.saveNewUser(new User()).getMyShareID()).isEqualTo(100043L);
    }

    @Test
    void anExistingMyShareIdIsKept() {
        User user = new User();
        user.setMyShareID(555L);

        assertThat(service.saveNewUser(user).getMyShareID()).isEqualTo(555L);
    }

    @Test
    void savingNeverChangesTheChurch() {
        User withoutChurch = new User();

        assertThat(service.save(withoutChurch).getChurch()).isNull();
    }
}
