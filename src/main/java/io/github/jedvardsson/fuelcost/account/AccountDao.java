package io.github.jedvardsson.fuelcost.account;

import com.google.protobuf.Timestamp;
import io.github.jedvardsson.fuelcost.common.Arguments;
import io.github.jedvardsson.fuelcost.common.PageTokens;
import io.github.jedvardsson.fuelcost.common.VersionEtag;
import io.github.jedvardsson.fuelcost.db.DbClient;
import io.github.jedvardsson.fuelcost.grpc.GrpcException;
import io.github.jedvardsson.fuelcost.v1.Account;
import io.github.jedvardsson.fuelcost.v1.CreateAccountRequest;
import io.github.jedvardsson.fuelcost.v1.DeleteAccountRequest;
import io.github.jedvardsson.fuelcost.v1.GetAccountRequest;
import io.github.jedvardsson.fuelcost.v1.ListAccountsRequest;
import io.github.jedvardsson.fuelcost.v1.ListAccountsResponse;
import io.github.jedvardsson.fuelcost.v1.UpdateAccountRequest;
import org.jdbi.v3.core.result.RowView;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AccountDao {

    private static final int MAX_PAGE_SIZE = 1000;
    private static final int DEFAULT_PAGE_SIZE = 100;

    private final DbClient dbClient;

    public AccountDao(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request) {
        return dbClient.withHandle(h -> {
            if (!request.hasAccount()) {
                throw GrpcException.requiredArgument("account");
            }

            return h.createQuery("""
                            insert into account (version, create_time, update_time)
                            values (:version, statement_timestamp(), statement_timestamp())
                            returning account_id, version, create_time, update_time
                            """)
                    .bind("version", 1)
                    .map(r -> request.getAccount().toBuilder()
                            .setName(getAccountName(r).toString())
                            .setEtag(formatVersionEtag(r))
                            .setCreateTime(r.getColumn("create_time", Timestamp.class))
                            .setUpdateTime(r.getColumn("update_time", Timestamp.class))
                            .build())
                    .one();
        });
    }

    @Transactional
    public Account updateAccount(UpdateAccountRequest request) {
        return dbClient.withHandle(h -> {
            if (!request.hasAccount()) {
                throw GrpcException.requiredArgument("account");
            }

            Account account = request.getAccount();
            String name = account.getName();
            AccountName key = Arguments.parse(name, "name", AccountName::parse);
            Long version = VersionEtag.tryParseVersion(account.getEtag());

            return h.createQuery("""
                            update account t set
                                version = version + 1,
                                update_time = statement_timestamp()
                            where t.account_id = :account_id and (:version is null or t.version = :version)
                            returning account_id, version, create_time, update_time
                            """)
                    .bind("account_id", key.accountId())
                    .bind("version", version)
                    .map(r -> request.getAccount().toBuilder()
                            .setName(getAccountName(r).toString())
                            .setEtag(formatVersionEtag(r))
                            .setCreateTime(r.getColumn("create_time", Timestamp.class))
                            .setUpdateTime(r.getColumn("update_time", Timestamp.class))
                            .build())
                    .findFirst()
                    .orElseThrow(() -> version == null ? GrpcException.notFound(name) : GrpcException.etagNotMatching(name));
        });
    }

    @Transactional
    public void deleteAccount(DeleteAccountRequest request) {
        String name = request.getName();
        AccountName key = Arguments.parse(name, "name", AccountName::parse);
        Long version = VersionEtag.parseOptionalVersion(request.getEtag()).orElse(null);
        dbClient.withHandle(h -> h.createQuery("""
                        delete from account t where t.account_id = :account_id and (:version is null or t.version = :version)
                        returning account_id, version
                        """)
                .bind("account_id", key.accountId())
                .bind("version", version)
                .mapTo(String.class)
                .findFirst()
                .orElseThrow(() -> version == null ? GrpcException.notFound(name) : GrpcException.etagNotMatching(name)));
    }

    @Transactional(readOnly = true)
    public Account getAccount(GetAccountRequest request) {
        String name = Arguments.requireNonEmpty(request.getName(), "name");
        return getAccount(name).orElseThrow(() -> GrpcException.notFound(name));
    }

    @Transactional(readOnly = true)
    public Optional<Account> getAccount(String name) {
        return getAccount(AccountName.parse(name));
    }

    @Transactional(readOnly = true)
    public Optional<Account> getAccount(AccountName name) {
        return getAccounts(List.of(name)).stream().findFirst();
    }

    private List<Account> getAccounts(List<AccountName> names) {
        return dbClient.withHandle(h -> h.createQuery("""
                        select
                            t.account_id,
                            t.version,
                            t.create_time,
                            t.update_time
                        from unnest(:account_ids) with ordinality as x(account_id, ord)
                        join account t on t.account_id = x.account_id
                        order by x.ord
                        """)
                .bind("account_ids", names.stream().mapToLong(AccountName::accountId).toArray())
                .map(r -> Account.newBuilder()
                        .setName(getAccountName(r).toString())
                        .setEtag(formatVersionEtag(r))
                        .setCreateTime(r.getColumn("create_time", Timestamp.class))
                        .setUpdateTime(r.getColumn("update_time", Timestamp.class))
                        .build())
                .list());
    }

    @NotNull
    private static String formatVersionEtag(RowView r) {
        return VersionEtag.format(r.getColumn("version", Long.class));
    }

    private static AccountName getAccountName(RowView r) {
        return new AccountName(r.getColumn("account_id", Long.class));
    }

    @Transactional(readOnly = true)
    public ListAccountsResponse listAccounts(ListAccountsRequest request) {
        AccountName pageToken = PageTokens.parseOptional(request.getPageToken(), AccountName.class).orElseGet(() -> new AccountName(0L));
        int pageSize = request.getPageSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(MAX_PAGE_SIZE, request.getPageSize());

        return dbClient.withHandle(h -> {
            List<AccountName> names = h.createQuery("""
                            select
                                t.account_id
                            from account t
                            where t.account_id > :page_token_account_id
                            order by t.account_id
                            limit :page_size
                            """)
                    .bind("page_token_account_id", pageToken.accountId())
                    .bind("page_size", pageSize)
                    .map(r -> getAccountName(r))
                    .list();

            var entities = getAccounts(names);
            int size = names.size();
            String nextPageToken = size != pageSize ? "" : PageTokens.format(names.get(size - 1));
            return ListAccountsResponse.newBuilder()
                    .addAllAccounts(entities)
                    .setNextPageToken(nextPageToken)
                    .build();
        });
    }
}
