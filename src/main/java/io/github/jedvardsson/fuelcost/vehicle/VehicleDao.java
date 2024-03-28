package io.github.jedvardsson.fuelcost.vehicle;

import com.google.protobuf.Timestamp;
import io.github.jedvardsson.fuelcost.account.AccountDao;
import io.github.jedvardsson.fuelcost.account.AccountName;
import io.github.jedvardsson.fuelcost.common.Arguments;
import io.github.jedvardsson.fuelcost.common.PageTokens;
import io.github.jedvardsson.fuelcost.common.VersionEtag;
import io.github.jedvardsson.fuelcost.db.DbClient;
import io.github.jedvardsson.fuelcost.grpc.GrpcException;
import io.github.jedvardsson.fuelcost.v1.CreateVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.DeleteVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.GetVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.ListVehiclesRequest;
import io.github.jedvardsson.fuelcost.v1.ListVehiclesResponse;
import io.github.jedvardsson.fuelcost.v1.UpdateVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.Vehicle;
import org.jdbi.v3.core.result.RowView;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VehicleDao {

    private static final int MAX_PAGE_SIZE = 1000;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int DISPLAY_NAME_MAX_LENGTH = 30;

    private final DbClient dbClient;
    private final AccountDao accountDao;

    public VehicleDao(DbClient dbClient, AccountDao accountDao) {
        this.dbClient = dbClient;
        this.accountDao = accountDao;
    }

    @Transactional
    public Vehicle createVehicle(CreateVehicleRequest request) {
        return dbClient.withHandle(h -> {
            if (!request.hasVehicle()) {
                throw GrpcException.requiredArgument("vehicle");
            }

            Vehicle vehicle = request.getVehicle();

            AccountName parentName = Arguments.parse(request.getParent(), "parent", AccountName::parse);
            requireExists(parentName);

            return h.createQuery("""
                            insert into vehicle (account_id, version, create_time, update_time, display_name)
                            values (:account_id, :version, statement_timestamp(), statement_timestamp(), :display_name)
                            returning account_id, vehicle_id, version, create_time, update_time
                            """)
                    .bind("account_id", parentName.accountId())
                    .bind("version", 1)
                    .bind("display_name", parseDisplayName(vehicle.getDisplayName()))
                    .map(r -> request.getVehicle().toBuilder()
                            .setName(getVehicleName(r).toString())
                            .setEtag(formatVersionEtag(r))
                            .setCreateTime(r.getColumn("create_time", Timestamp.class))
                            .setUpdateTime(r.getColumn("update_time", Timestamp.class))
                            .build())
                    .one();
        });
    }


    @Transactional
    public Vehicle updateVehicle(UpdateVehicleRequest request) {
        return dbClient.withHandle(h -> {
            if (!request.hasVehicle()) {
                throw GrpcException.requiredArgument("vehicle");
            }

            Vehicle vehicle = request.getVehicle();
            String name = vehicle.getName();
            VehicleName key = Arguments.parse(name, "name", VehicleName::parse);
            Long version = VersionEtag.tryParseVersion(vehicle.getEtag());

            return h.createQuery("""
                            update vehicle t set
                                version = version + 1,
                                update_time = statement_timestamp(),
                                display_name = :display_name
                            where t.account_id = :account_id and t.vehicle_id = :vehicle_id and (:version is null or t.version = :version)
                            returning account_id, vehicle_id, version, create_time, update_time
                            """)
                    .bind("account_id", key.accountId())
                    .bind("vehicle_id", key.vehicleId())
                    .bind("version", version)
                    .bind("display_name", parseDisplayName(vehicle.getDisplayName()))
                    .map(r -> request.getVehicle().toBuilder()
                            .setName(getVehicleName(r).toString())
                            .setEtag(formatVersionEtag(r))
                            .setCreateTime(r.getColumn("create_time", Timestamp.class))
                            .setUpdateTime(r.getColumn("update_time", Timestamp.class))
                            .build())
                    .findFirst()
                    .orElseThrow(() -> version == null ? GrpcException.notFound(name) : GrpcException.etagNotMatching(name));
        });
    }

    private static String parseDisplayName(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        if (s.length() > DISPLAY_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("display_name must be less than " + DISPLAY_NAME_MAX_LENGTH + " characters: " + s.substring(0, DISPLAY_NAME_MAX_LENGTH) + "...");
        }
        return s;
    }

    @Transactional
    public void deleteVehicle(DeleteVehicleRequest request) {
        String name = request.getName();
        VehicleName key = Arguments.parse(name, "name", VehicleName::parse);
        Long version = VersionEtag.parseOptionalVersion(request.getEtag()).orElse(null);
        dbClient.withHandle(h -> h.createQuery("""
                        delete from vehicle t where t.account_id = :account_id and t.vehicle_id = :vehicle_id and (:version is null or t.version = :version)
                        returning account_id, vehicle_id, version
                        """)
                .bind("account_id", key.accountId())
                .bind("vehicle_id", key.vehicleId())
                .bind("version", version)
                .mapTo(String.class)
                .findFirst()
                .orElseThrow(() -> version == null ? GrpcException.notFound(name) : GrpcException.etagNotMatching(name)));
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicle(GetVehicleRequest request) {
        String name = Arguments.requireNonEmpty(request.getName(), "name");
        return getVehicle(name).orElseThrow(() -> GrpcException.notFound(name));
    }

    @Transactional(readOnly = true)
    public Optional<Vehicle> getVehicle(String name) {
        return getVehicles(List.of(VehicleName.parse(name))).stream().findFirst();
    }

    private List<Vehicle> getVehicles(List<VehicleName> names) {
        return dbClient.withHandle(h -> h.createQuery("""
                        select
                            t.account_id,
                            t.vehicle_id,
                            t.version,
                            t.create_time,
                            t.update_time,
                            t.display_name
                        from unnest(:account_ids, :vehicle_ids) with ordinality as x(account_id, vehicle_id, ord)
                        join vehicle t on t.account_id = x.account_id and t.vehicle_id = x.vehicle_id
                        order by x.ord
                        """)
                .bind("account_ids", names.stream().mapToLong(VehicleName::accountId).toArray())
                .bind("vehicle_ids", names.stream().mapToLong(VehicleName::vehicleId).toArray())
                .map(r -> {
                    Vehicle.Builder b = Vehicle.newBuilder();
                    b.setName(getVehicleName(r).toString())
                            .setEtag(formatVersionEtag(r))
                            .setCreateTime(r.getColumn("create_time", Timestamp.class))
                            .setUpdateTime(r.getColumn("update_time", Timestamp.class))
                            .build();
                    String displayName = r.getColumn("display_name", String.class);
                    if (displayName != null) {
                        b.setDisplayName(displayName);
                    }
                    return b.build();
                })
                .list());
    }

    @NotNull
    private static String formatVersionEtag(RowView r) {
        return VersionEtag.format(r.getColumn("version", Long.class));
    }

    private static VehicleName getVehicleName(RowView r) {
        return new VehicleName(r.getColumn("account_id", Long.class), r.getColumn("vehicle_id", Long.class));
    }

    @Transactional(readOnly = true)
    public ListVehiclesResponse listVehicles(ListVehiclesRequest request) {
        AccountName parentName = Arguments.parse(request.getParent(), "parent", AccountName::parse);
        requireExists(parentName);

        VehicleName pageToken = PageTokens.parseOptional(request.getPageToken(), VehicleName.class).orElseGet(() -> new VehicleName(parentName.accountId(), 0L));
        int pageSize = request.getPageSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(MAX_PAGE_SIZE, request.getPageSize());

        return dbClient.withHandle(h -> {
            List<VehicleName> names = h.createQuery("""
                            select
                                t.account_id,
                                t.vehicle_id
                            from vehicle t
                            where
                                t.account_id = :parent_account_id
                                and t.vehicle_id > :page_token_vehicle_id
                            order by t.account_id, t.vehicle_id
                            limit :page_size
                            """)
                    .bind("parent_account_id", parentName.accountId())
                    .bind("page_token_vehicle_id", pageToken.vehicleId())
                    .bind("page_size", pageSize)
                    .map(r -> getVehicleName(r))
                    .list();

            var entities = getVehicles(names);
            int size = names.size();
            String nextPageToken = size != pageSize ? "" : PageTokens.format(names.get(size - 1));
            return ListVehiclesResponse.newBuilder()
                    .addAllVehicles(entities)
                    .setNextPageToken(nextPageToken)
                    .build();
        });
    }

    private void requireExists(AccountName parentName) {
        accountDao.getAccount(parentName).orElseThrow(() -> GrpcException.notFound(parentName.toString()));
    }
}
