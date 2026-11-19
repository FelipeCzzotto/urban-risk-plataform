package br.com.urbanrisk.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Sha256ReporterHasherTest {

    @Test
    void hashIsHexSha256OfPepperPlusContact() {
        // printf 'peppermaria@example.com' | sha256sum
        assertThat(new Sha256ReporterHasher("pepper").hash("maria@example.com"))
                .isEqualTo("ef646d0b70e1118a18ddf55fac9e8ab527114ac3dda94f6cb20295636bbcb696");
    }

    @Test
    void differentPepperDifferentHash() {
        assertThat(new Sha256ReporterHasher("a").hash("x")).isNotEqualTo(new Sha256ReporterHasher("b").hash("x"));
    }

    @Test
    void blankPepperRejected() {
        assertThatThrownBy(() -> new Sha256ReporterHasher(" ")).isInstanceOf(IllegalStateException.class);
    }
}
