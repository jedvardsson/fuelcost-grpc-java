package io.github.jedvardsson.fuelcost.vehicle;

import com.google.protobuf.Empty;
import io.github.jedvardsson.fuelcost.grpc.GrpcUtil;
import io.github.jedvardsson.fuelcost.v1.CreateVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.DeleteVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.GetVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.ListVehiclesRequest;
import io.github.jedvardsson.fuelcost.v1.ListVehiclesResponse;
import io.github.jedvardsson.fuelcost.v1.UpdateVehicleRequest;
import io.github.jedvardsson.fuelcost.v1.Vehicle;
import io.github.jedvardsson.fuelcost.v1.VehicleServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;


@Service
public class VehicleService extends VehicleServiceGrpc.VehicleServiceImplBase {

    private final VehicleDao vehicleDao;

    public VehicleService(VehicleDao vehicleDao) {
        this.vehicleDao = vehicleDao;
    }

    @Override
    public void createVehicle(CreateVehicleRequest request, StreamObserver<Vehicle> responseObserver) {
        GrpcUtil.handleResponse(responseObserver, () -> vehicleDao.createVehicle(request));
    }

    @Override
    public void updateVehicle(UpdateVehicleRequest request, StreamObserver<Vehicle> responseObserver) {
        GrpcUtil.handleResponse(responseObserver, () -> vehicleDao.updateVehicle(request));
    }

    @Override
    public void deleteVehicle(DeleteVehicleRequest request, StreamObserver<Empty> responseObserver) {
        GrpcUtil.handleResponse(responseObserver, () -> {
            vehicleDao.deleteVehicle(request);
            return Empty.getDefaultInstance();
        });
    }

    @Override
    public void getVehicle(GetVehicleRequest request, StreamObserver<Vehicle> responseObserver) {
        GrpcUtil.handleResponse(responseObserver, () -> vehicleDao.getVehicle(request));
    }

    @Override
    public void listVehicles(ListVehiclesRequest request, StreamObserver<ListVehiclesResponse> responseObserver) {
        GrpcUtil.handleResponse(responseObserver, () -> vehicleDao.listVehicles(request));
    }

}
