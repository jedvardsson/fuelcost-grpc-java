
package io.github.jedvardsson.fuelcost.account;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.jedvardsson.fuelcost.common.Arguments;
import io.github.jedvardsson.fuelcost.common.PatchedPathTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;

public record AccountName(long accountId) implements Comparable<AccountName> {
    private static final PatchedPathTemplate TEMPLATE = PatchedPathTemplate.create("accounts/{account}");
    public static final Comparator<AccountName> COMPARATOR = Comparator.comparing(AccountName::accountId);

    private AccountName(Map<String, String> map) {
        this(Arguments.parseAccountId(map.get("account"), "account"));
    }

    @JsonCreator
    public static AccountName parse(String name) {
        Map<String, String> m = TEMPLATE.parse(name);
        return new AccountName(m);
    }

    public static String format(long accountId) {
        return new AccountName(accountId).toString();
    }

    @JsonValue
    public String toString() {
        return TEMPLATE.instantiate("account", Long.toString(accountId));
    }

    @Override
    public int compareTo(@NotNull AccountName o) {
        return COMPARATOR.compare(this, o);
    }
}
