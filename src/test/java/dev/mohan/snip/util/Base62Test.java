package dev.mohan.snip.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62Test {

    @Test
    void encodesKnownValues() {
        assertThat(Base62.encode(0)).isEqualTo("0");
        assertThat(Base62.encode(61)).isEqualTo("Z");
        assertThat(Base62.encode(62)).isEqualTo("10");
        assertThat(Base62.encode(3843)).isEqualTo("ZZ");
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 42, 1_000_000, 123_456_789L, Long.MAX_VALUE})
    void roundTrip(long value) {
        assertThat(Base62.decode(Base62.encode(value))).isEqualTo(value);
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> Base62.decode("ab-c")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Base62.encode(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
