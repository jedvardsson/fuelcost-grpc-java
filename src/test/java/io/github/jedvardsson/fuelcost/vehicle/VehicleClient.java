package io.github.jedvardsson.fuelcost.vehicle;

import io.github.jedvardsson.fuelcost.grpc.GrpcChannelWrapper;
import io.github.jedvardsson.fuelcost.v1.CreateVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.DeleteVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.GetVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.ListVehiclesRequest;
import io.github.jedvardsson.fuelcost.v1.ListVehiclesResponse;
import io.github.jedvardsson.fuelcost.v1.UpdateVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.Vehicle;
import io.github.jedvardsson.fuelcost.v1.VehicleServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class VehicleClient {

    private final VehicleServiceGrpc.VehicleServiceBlockingStub vehicleStub;

    @Autowired
    public VehicleClient(GrpcChannelWrapper wrapper) {
        vehicleStub = VehicleServiceGrpc.newBlockingStub(wrapper.getChannel());
    }

    public Vehicle createVehicle(String parent, Vehicle vehicle) {
        return vehicleStub.createVehicle(CreateVehicleRequest.newBuilder().setParent(parent).setVehicle(vehicle).build());
    }

    public Vehicle createVehicle(CreateVehicleRequest request) {
        return vehicleStub.createVehicle(request);
    }

    public Vehicle updateVehicle(UpdateVehicleRequest request) {
        return vehicleStub.updateVehicle(request);
    }

    public Vehicle updateVehicle(Vehicle vehicle) {
        return updateVehicle(UpdateVehicleRequest.newBuilder().setVehicle(vehicle).build());
    }

    public void deleteVehicle(DeleteVehicleRequest request) {
        //noinspection ResultOfMethodCallIgnored
        vehicleStub.deleteVehicle(request);
    }

    public void deleteVehicle(String name) {
        deleteVehicle(DeleteVehicleRequest.newBuilder().setName(name).build());
    }

    public void deleteVehicle(String name, String etag) {
        deleteVehicle(DeleteVehicleRequest.newBuilder().setName(name).setEtag(etag).build());
    }

    public Vehicle getVehicle(GetVehicleRequest request) {
        return vehicleStub.getVehicle(request);
    }

    public Vehicle getVehicle(String name) {
        return getVehicle(GetVehicleRequest.newBuilder().setName(name).build());
    }

    public ListVehiclesResponse listVehicles(ListVehiclesRequest request) {
        return vehicleStub.listVehicles(request);
    }

    public Stream<ListVehiclesResponse> streamVehicles(ListVehiclesRequest request) {
        return Stream.iterate(
                listVehicles(request),
                l -> !ListVehiclesResponse.getDefaultInstance().equals(l),
                l -> l.getNextPageToken().isEmpty() ? ListVehiclesResponse.getDefaultInstance() : listVehicles(request.toBuilder().setPageToken(l.getNextPageToken()).build()));
    }

    public Stream<List<Vehicle>> streamVehicles(String parent, int pageSize) {
        ListVehiclesRequest request = ListVehiclesRequest.newBuilder().setParent(parent).setPageSize(pageSize).build();
        return streamVehicles(request).map(ListVehiclesResponse::getVehiclesList);
    }
}
