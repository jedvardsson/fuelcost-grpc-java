package io.github.jedvardsson.fuelcost.account;

import com.google.common.primitives.Ints;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.BaseEncoding;

class AccountDaoTest {

    @Test
    void test() {
        for (int i = 0; i < 1000; i += 17) {
            String encoded = BaseEncoding.base32().encode(Ints.toByteArray(i));
            System.out.printf("%d5i: %s%n", i, encoded);
        }
    }
}