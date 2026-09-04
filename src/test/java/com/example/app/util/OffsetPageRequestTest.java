package com.example.app.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OffsetPageRequestTest {

  @Test
  void rejectsLimitAboveMaximum() {
    assertThatThrownBy(() -> OffsetPageRequest.of(0, OffsetPageRequest.MAX_LIMIT + 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNegativeOffset() {
    assertThatThrownBy(() -> OffsetPageRequest.of(-1, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsLimitAtMaximum() {
    OffsetPageRequest request = OffsetPageRequest.of(0, OffsetPageRequest.MAX_LIMIT);
    assertThat(request.getPageSize()).isEqualTo(OffsetPageRequest.MAX_LIMIT);
  }
}
