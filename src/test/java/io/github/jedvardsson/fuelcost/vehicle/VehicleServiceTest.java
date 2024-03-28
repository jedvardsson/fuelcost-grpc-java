package io.github.jedvardsson.fuelcost.vehicle;

import com.google.protobuf.util.Timestamps;
import io.github.jedvardsson.fuelcost.ApplicationTestConfig;
import io.github.jedvardsson.fuelcost.account.AccountClient;
import io.github.jedvardsson.fuelcost.common.VersionEtag;
import io.github.jedvardsson.fuelcost.grpc.GrpcAssertions;
import io.github.jedvardsson.fuelcost.v1.Account;
import io.github.jedvardsson.fuelcost.v1.CreateVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.Vehicle;
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
class VehicleServiceTest {

    private final VehicleClient client;
    private final Account account1;
    private final Account account2;


    @Autowired
    public VehicleServiceTest(VehicleClient client, AccountClient accountClient) {
        this.client = client;
        account1 = accountClient.createEmptyAccount();
        account2 = accountClient.createEmptyAccount();
    }

    private CreateVehicleRequest newCreateVehicleRequest() {
        return CreateVehicleRequest.newBuilder()
                .setParent(account1.getName())
                .setVehicle(Vehicle.newBuilder()
                        .build())
                .build();
    }

    private Vehicle newVehicleForUpdate(Vehicle entity) {
        return entity.toBuilder()
                .build();
    }

    @Test
    public void testSaveBase() {
        Vehicle v0 = Vehicle.newBuilder()
                .setDisplayName("some display name")
                .build();

        // test create
        Vehicle v1 = client.createVehicle(account1.getName(), v0);
        assertEquals(v0, v1.toBuilder().clearName().clearEtag().clearCreateTime().clearUpdateTime().build());

        // test update
        Vehicle v2 = v1.toBuilder()
                .clearDisplayName()
                .build();

        Vehicle v3 = client.updateVehicle(v2);
        assertEquals(v0, v1.toBuilder().clearName().clearEtag().clearCreateTime().clearUpdateTime().build());
        assertEquals(v3, client.getVehicle(v1.getName()));

        // test delete
        client.deleteVehicle(v1.getName());
        GrpcAssertions.assertThrows(Status.Code.NOT_FOUND, () -> client.getVehicle(v1.getName()));
    }


    @Test
    void testGet() {
        Vehicle e1 = client.createVehicle(newCreateVehicleRequest());
        assertEquals(e1, client.getVehicle(e1.getName()));
    }

    @Test
    void testCreate() {
        CreateVehicleRequest e0 = newCreateVehicleRequest();
        Vehicle e1 = client.createVehicle(e0);

        assertNotNull(e1.getCreateTime());
        assertEquals(e1.getCreateTime(), e1.getUpdateTime());

        // Sanity check if other fields have been saved
        Vehicle expected = e0.getVehicle().toBuilder()
                .setName(e1.getName())
                .setCreateTime(e1.getCreateTime())
                .setUpdateTime(e1.getUpdateTime())
                .setEtag(VersionEtag.of(1).toString())
                .build();
        assertEquals(expected, e1);
    }

    @Test
    void testUpdate() {
        Vehicle e1 = client.createVehicle(newCreateVehicleRequest());
        Vehicle e2 = newVehicleForUpdate(e1);
        Vehicle e3 = client.updateVehicle(e2);

        Assertions.assertTrue(Timestamps.compare(e1.getCreateTime(), e3.getUpdateTime()) < 0);

        // Sanity check to ensure that other fields are saved
        Vehicle expected = e2.toBuilder()
                .setUpdateTime(e3.getUpdateTime())
                .setEtag(VersionEtag.parse(e1.getEtag()).increment().toString())
                .build();
        assertEquals(expected, e3);
    }

    @Test
    void testUpdate_NoEtag() {
        Vehicle e1 = client.createVehicle(newCreateVehicleRequest());
        Vehicle e2 = client.updateVehicle(e1.toBuilder().clearEtag().build());
        Assertions.assertEquals(VersionEtag.parse(e1.getEtag()).increment(), VersionEtag.parse(e2.getEtag()));
    }

    @Test
    void testUpdate_NotFound() {
        Vehicle e1 = client.createVehicle(newCreateVehicleRequest());
        client.deleteVehicle(e1.getName(), e1.getEtag());
        GrpcAssertions.assertThrows(Status.Code.NOT_FOUND, () -> client.updateVehicle(e1.toBuilder().clearEtag().build()));
    }

    @Test
    void testUpdate_ConcurrentModification() {
        Vehicle e1 = client.createVehicle(newCreateVehicleRequest());
        GrpcAssertions.assertThrows(Status.Code.ABORTED, () -> client.updateVehicle(e1.toBuilder().setEtag(VersionEtag.of(99).toString()).build()));
    }


    @Test
    void testDelete() {
        Vehicle e1 = client.createVehicle(newCreateVehicleRequest());
        client.deleteVehicle(e1.getName(), e1.getEtag());
        GrpcAssertions.assertThrows(Status.Code.NOT_FOUND, () -> client.getVehicle(e1.getName()));
    }

    @Test
    void testDelete_NoEtag() {
        Vehicle e1 = client.createVehicle(newCreateVehicleRequest());
        client.deleteVehicle(e1.getName());
        GrpcAssertions.assertThrows(Status.Code.NOT_FOUND, () -> client.getVehicle(e1.getName()));
    }

    @Test
    void testDelete_NotFound() {
        Vehicle e1 = client.createVehicle(newCreateVehicleRequest());
        client.deleteVehicle(e1.getName(), e1.getEtag());
        GrpcAssertions.assertThrows(Status.Code.NOT_FOUND, () -> client.deleteVehicle(e1.getName()));
    }

    @Test
    void testDelete_ConcurrentModification() {
        Vehicle e1 = client.createVehicle(newCreateVehicleRequest());
        GrpcAssertions.assertThrows(Status.Code.ABORTED, () -> client.deleteVehicle(e1.getName(), VersionEtag.of(0).toString()));
    }

    @Test
    void testList_Ok() {
        // Create some extra vehicles on other account
        IntStream.range(0, 9).forEach(i -> client.createVehicle(newCreateVehicleRequest().toBuilder().setParent(account2.getName()).build()));

        List<Vehicle> expected = IntStream.range(0, 9).mapToObj(i -> client.createVehicle(newCreateVehicleRequest()))
                .sorted(comparing(Vehicle::getName))
                .toList();
        Set<String> names = expected.stream().map(Vehicle::getName).collect(Collectors.toSet());

        List<Vehicle> actual = client.streamVehicles(account1.getName(), 5)
                .flatMap(List::stream)
                .filter(e -> names.contains(e.getName()))
                .sorted(comparing(Vehicle::getName))
                .toList();

        assertEquals(expected, actual);
    }
}