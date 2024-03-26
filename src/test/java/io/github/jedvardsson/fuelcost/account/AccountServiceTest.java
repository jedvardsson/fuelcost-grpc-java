package io.github.jedvardsson.fuelcost.account;

import com.google.protobuf.util.Timestamps;
import io.github.jedvardsson.fuelcost.ApplicationTestConfig;
import io.github.jedvardsson.fuelcost.common.VersionEtag;
import io.github.jedvardsson.fuelcost.grpc.GrpcAssertions;
import io.github.jedvardsson.fuelcost.v1.Account;
import io.github.jedvardsson.fuelcost.v1.CreateAccountRequest;
import io.grpc.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("ThrowableNotThrown")
@SpringBootTest(classes = ApplicationTestConfig.class)
@ActiveProfiles({"test"})
class AccountServiceTest {

    private final AccountClient client;


    @Autowired
    public AccountServiceTest(AccountClient client) {
        this.client = client;
    }

    private CreateAccountRequest newCreateAccountRequest() {
        return CreateAccountRequest.newBuilder()
                .setAccount(Account.newBuilder()
                        .build())
                .build();
    }

    private Account newAccountForUpdate(Account entity) {
        return entity.toBuilder()
                .build();
    }

    @Test
    void testGet() {
        Account e1 = client.createAccount(newCreateAccountRequest());
        assertEquals(e1, client.getAccount(e1.getName()));
    }

    @Test
    void testCreate() {
        CreateAccountRequest e0 = newCreateAccountRequest();
        Account e1 = client.createAccount(e0);

        assertNotNull(e1.getCreateTime());
        assertEquals(e1.getCreateTime(), e1.getUpdateTime());

        // Sanity check if other fields have been saved
        Account expected = e0.getAccount().toBuilder()
                .setName(e1.getName())
                .setCreateTime(e1.getCreateTime())
                .setUpdateTime(e1.getUpdateTime())
                .setEtag(VersionEtag.of(1).toString())
                .build();
        assertEquals(expected, e1);
    }

    @Test
    void testUpdate() {
        Account e1 = client.createAccount(newCreateAccountRequest());
        Account e2 = newAccountForUpdate(e1);
        Account e3 = client.updateAccount(e2);

        Assertions.assertTrue(Timestamps.compare(e1.getCreateTime(), e3.getUpdateTime()) < 0);

        // Sanity check to ensure that other fields are saved
        Account expected = e2.toBuilder()
                .setUpdateTime(e3.getUpdateTime())
                .setEtag(VersionEtag.parse(e1.getEtag()).increment().toString())
                .build();
        assertEquals(expected, e3);
    }

    @Test
    void testUpdate_NoEtag() {
        Account e1 = client.createAccount(newCreateAccountRequest());
        Account e2 = client.updateAccount(e1.toBuilder().clearEtag().build());
        Assertions.assertEquals(VersionEtag.parse(e1.getEtag()).increment(), VersionEtag.parse(e2.getEtag()));
    }

    @Test
    void testUpdate_NotFound() {
        Account e1 = client.createAccount(newCreateAccountRequest());
        client.deleteAccount(e1.getName(), e1.getEtag());
        GrpcAssertions.assertThrows(Status.Code.NOT_FOUND, () -> client.updateAccount(e1.toBuilder().clearEtag().build()));
    }

    @Test
    void testUpdate_ConcurrentModification() {
        Account e1 = client.createAccount(newCreateAccountRequest());
        GrpcAssertions.assertThrows(Status.Code.ABORTED, () -> client.updateAccount(e1.toBuilder().setEtag(VersionEtag.of(99).toString()).build()));
    }


    @Test
    void testDelete() {
        Account e1 = client.createAccount(newCreateAccountRequest());
        client.deleteAccount(e1.getName(), e1.getEtag());
        GrpcAssertions.assertThrows(Status.Code.NOT_FOUND, () -> client.getAccount(e1.getName()));
    }

    @Test
    void testDelete_NoEtag() {
        Account e1 = client.createAccount(newCreateAccountRequest());
        client.deleteAccount(e1.getName());
        GrpcAssertions.assertThrows(Status.Code.NOT_FOUND, () -> client.getAccount(e1.getName()));
    }

    @Test
    void testDelete_NotFound() {
        Account e1 = client.createAccount(newCreateAccountRequest());
        client.deleteAccount(e1.getName(), e1.getEtag());
        GrpcAssertions.assertThrows(Status.Code.NOT_FOUND, () -> client.deleteAccount(e1.getName()));
    }

    @Test
    void testDelete_ConcurrentModification() {
        Account e1 = client.createAccount(newCreateAccountRequest());
        GrpcAssertions.assertThrows(Status.Code.ABORTED, () -> client.deleteAccount(e1.getName(), VersionEtag.of(0).toString()));
    }

    @Test
    void testList_Ok() {
        List<Account> expected = IntStream.range(0, 9).mapToObj(i -> client.createAccount(newCreateAccountRequest()))
                .sorted(comparing(Account::getName))
                .toList();
        Set<String> names = expected.stream().map(Account::getName).collect(Collectors.toSet());

        List<Account> actual = client.streamAccounts(5)
                .flatMap(List::stream)
                .filter(e -> names.contains(e.getName()))
                .sorted(comparing(Account::getName))
                .toList();

        assertEquals(expected, actual);
    }
}