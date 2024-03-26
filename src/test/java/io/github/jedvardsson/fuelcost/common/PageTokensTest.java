package io.github.jedvardsson.fuelcost.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PageTokensTest {

    @Test
    void test() {
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        record SampleRec(String a, long b) {
        }

        SampleRec rec = new SampleRec("a", 3);
        String token = PageTokens.format(rec);
        Assertions.assertEquals(rec, PageTokens.parseOptional(token, SampleRec.class).orElse(null));
    }

}