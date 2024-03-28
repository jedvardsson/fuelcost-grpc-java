
package io.github.jedvardsson.fuelcost.vehicle;

import io.github.jedvardsson.fuelcost.common.Arguments;
import io.github.jedvardsson.fuelcost.common.PatchedPathTemplate;

import java.util.Map;

public record VehicleName(long accountId, long vehicleId) {
    private static final PatchedPathTemplate TEMPLATE = PatchedPathTemplate.create("accounts/{account}/vehicles/{vehicle}");

    private VehicleName(String account, String vehicle) {
        this(Arguments.parseAccountId(account, "account"), Arguments.parseVehicleId(vehicle, "vehicle"));
    }

    private VehicleName(Map<String, String> map) {
        this(map.get("account"), map.get("vehicle"));
    }

    public static VehicleName parse(String name) {
        Map<String, String> m = TEMPLATE.parse(name);
        return new VehicleName(m);
    }

    @Override
    public String toString() {
        return TEMPLATE.instantiate("account", Long.toString(accountId), "vehicle", Long.toString(vehicleId));
    }
}
