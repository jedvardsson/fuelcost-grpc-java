package io.github.jedvardsson.fuelcost.account;

import com.google.protobuf.Empty;
import io.github.jedvardsson.fuelcost.grpc.GrpcUtil;
import io.github.jedvardsson.fuelcost.v1.Account;
import io.github.jedvardsson.fuelcost.v1.AccountServiceGrpc;
import io.github.jedvardsson.fuelcost.v1.CreateAccountRequest;
import io.github.jedvardsson.fuelcost.v1.DeleteAccountRequest;
import io.github.jedvardsson.fuelcost.v1.GetAccountRequest;
import io.github.jedvardsson.fuelcost.v1.ListAccountsRequest;
import io.github.jedvardsson.fuelcost.v1.ListAccountsResponse;
import io.github.jedvardsson.fuelcost.v1.UpdateAccountRequest;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;


@Service
public class AccountService extends AccountServiceGrpc.AccountServiceImplBase {

    private final io.github.jedvardsson.fuelcost.account.AccountDao accountDao;

    public AccountService(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<Account> responseObserver) {
        GrpcUtil.handleResponse(responseObserver, () -> accountDao.createAccount(request));
    }

    @Override
    public void updateAccount(UpdateAccountRequest request, StreamObserver<Account> responseObserver) {
        GrpcUtil.handleResponse(responseObserver, () -> accountDao.updateAccount(request));
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<Empty> responseObserver) {
        GrpcUtil.handleResponse(responseObserver, () -> {
            accountDao.deleteAccount(request);
            return Empty.getDefaultInstance();
        });
    }

    @Override
    public void getAccount(GetAccountRequest request, StreamObserver<Account> responseObserver) {
        GrpcUtil.handleResponse(responseObserver, () -> accountDao.getAccount(request));
    }

    @Override
    public void listAccounts(ListAccountsRequest request, StreamObserver<ListAccountsResponse> responseObserver) {
        GrpcUtil.handleResponse(responseObserver, () -> accountDao.listAccounts(request));
    }

}
