package io.github.jedvardsson.fuelcost.account;

import io.github.jedvardsson.fuelcost.grpc.GrpcChannelWrapper;
import io.github.jedvardsson.fuelcost.v1.Account;
import io.github.jedvardsson.fuelcost.v1.AccountServiceGrpc;
import io.github.jedvardsson.fuelcost.v1.CreateAccountRequest;
import io.github.jedvardsson.fuelcost.v1.DeleteAccountRequest;
import io.github.jedvardsson.fuelcost.v1.GetAccountRequest;
import io.github.jedvardsson.fuelcost.v1.ListAccountsRequest;
import io.github.jedvardsson.fuelcost.v1.ListAccountsResponse;
import io.github.jedvardsson.fuelcost.v1.UpdateAccountRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class AccountClient {

    private final AccountServiceGrpc.AccountServiceBlockingStub accountStub;

    @Autowired
    public AccountClient(GrpcChannelWrapper wrapper) {
        accountStub = AccountServiceGrpc.newBlockingStub(wrapper.getChannel());
    }

    public Account createAccount(Account account) {
        return accountStub.createAccount(CreateAccountRequest.newBuilder().setAccount(account).build());
    }

    public Account createAccount(CreateAccountRequest request) {
        return accountStub.createAccount(request);
    }

    public Account updateAccount(UpdateAccountRequest request) {
        return accountStub.updateAccount(request);
    }

    public Account updateAccount(Account account) {
        return updateAccount(UpdateAccountRequest.newBuilder().setAccount(account).build());
    }

    public void deleteAccount(DeleteAccountRequest request) {
        //noinspection ResultOfMethodCallIgnored
        accountStub.deleteAccount(request);
    }

    public void deleteAccount(String name) {
        deleteAccount(DeleteAccountRequest.newBuilder().setName(name).build());
    }

    public void deleteAccount(String name, String etag) {
        deleteAccount(DeleteAccountRequest.newBuilder().setName(name).setEtag(etag).build());
    }

    public Account getAccount(GetAccountRequest request) {
        return accountStub.getAccount(request);
    }

    public Account getAccount(String name) {
        return getAccount(GetAccountRequest.newBuilder().setName(name).build());
    }

    public ListAccountsResponse listAccounts(ListAccountsRequest request) {
        return accountStub.listAccounts(request);
    }

    public Stream<ListAccountsResponse> streamAccounts(ListAccountsRequest request) {
        return Stream.iterate(
                listAccounts(request),
                l -> !ListAccountsResponse.getDefaultInstance().equals(l),
                l -> l.getNextPageToken().isEmpty() ? ListAccountsResponse.getDefaultInstance() : listAccounts(request.toBuilder().setPageToken(l.getNextPageToken()).build()));
    }

    public Stream<List<Account>> streamAccounts(int pageSize) {
        ListAccountsRequest request = ListAccountsRequest.newBuilder().setPageSize(pageSize).build();
        return streamAccounts(request).map(ListAccountsResponse::getAccountsList);
    }
}
