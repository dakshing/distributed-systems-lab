package com.dsl.common.baseencoder;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    void testEncodeDecodeRoundTrip() {
        long originalId = 178263819283046400L;

        String shortUrl = Base62Encoder.encode(originalId);
        long decodedId = Base62Encoder.decode(shortUrl);

        System.out.println("Original: " + originalId);
        System.out.println("Encoded:  " + shortUrl);
        System.out.println("Decoded:  " + decodedId);

        assertEquals(originalId, decodedId, "Decoded ID must match the original ID");
    }

    @Test
    void testSimpleValues() {
        // 0 -> "0"
        assertEquals("0", Base62Encoder.encode(0));
        assertEquals(0, Base62Encoder.decode("0"));

        // 61 -> "Z"
        assertEquals("Z", Base62Encoder.encode(61));
        assertEquals(61, Base62Encoder.decode("Z"));

        // 62 -> "10"
        assertEquals("10", Base62Encoder.encode(62));
        assertEquals(62, Base62Encoder.decode("10"));
    }

    @Test
    void testInvalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode("abc$"));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode(""));
    }
}