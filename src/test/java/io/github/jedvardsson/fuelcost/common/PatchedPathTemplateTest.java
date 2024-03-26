package io.github.jedvardsson.fuelcost.common;

import com.google.api.pathtemplate.PathTemplate;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PatchedPathTemplateTest {


    @Test
    void testToString() {
        String pattern = "providers/{provider}/programs/{program}";
        PathTemplate template = PathTemplate.create(pattern);

        Assertions.assertNotEquals(pattern, template.toString(), "if this equal bug has been fixed");

        PatchedPathTemplate patchedTemplate = PatchedPathTemplate.create(pattern);
        Assertions.assertEquals(pattern, patchedTemplate.toString());
    }


    @Test
    void testInstantiate_encodeSegment() {
        PatchedPathTemplate template = PatchedPathTemplate.create("programs/{program}");
        Assertions.assertEquals("programs/abc~%23%2F%20+xyz", template.instantiate("program", "abc~#/ +xyz"));
    }

    @Test
    void testMatch_decodeSegment() {
        PatchedPathTemplate template = PatchedPathTemplate.create("programs/{program}");
        Assertions.assertEquals(ImmutableMap.of("program", "abc~#/ +xyz"), template.match("programs/abc~%23%2F%20+xyz"));
        Assertions.assertEquals(ImmutableMap.of("program", "abc~#/ +xyz"), template.match("programs/abc~#%2F %2Bxyz"));
    }

    @Test
    void testInstantiate_encodePath() {
        PatchedPathTemplate template = PatchedPathTemplate.create("{parent=**}/operations/{operation}");
        Assertions.assertEquals("providers/123/programs/abc~%23%2F%20xyz/operations/1", template.instantiate("parent", "providers/123/programs/abc~%23%2F%20xyz", "operation", "1"));
        Assertions.assertEquals("providers/123/programs/abc~%23%2F%20xyz/operations/1", template.instantiate("parent", "providers/123/programs/abc~#%2F xyz", "operation", "1"));
    }

    @Test
    void testMatch_decodePath() {
        PatchedPathTemplate template = PatchedPathTemplate.create("{parent=**}/operations/{operation}");
        Assertions.assertEquals(ImmutableMap.of("parent", "providers/123/programs/abc~%23%2F%20xyz", "operation", "1"),
                template.match("providers/123/programs/abc~%23%2F%20xyz/operations/1"));
        Assertions.assertEquals(ImmutableMap.of("parent", "providers/123/programs/abc~%23%2F%20xyz", "operation", "1"),
                template.match("providers/123/programs/abc~#%2F xyz/operations/1"));
    }
}
