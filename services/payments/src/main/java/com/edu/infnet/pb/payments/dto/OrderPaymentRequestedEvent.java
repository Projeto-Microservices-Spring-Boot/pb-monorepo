package com.edu.infnet.pb.payments.dto;

import jakarta.validation.constraints.DecimalMin;
  import jakarta.validation.constraints.NotNull;

  import java.math.BigDecimal;
  import java.util.UUID;

public record OrderPaymentRequestedEvent(

      @NotNull(message = "orderId is required")
      UUID orderId,

      @NotNull(message = "userId is required")
      UUID userId,

      @NotNull(message = "Amount is required")
      @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
      BigDecimal amount
  ) {
}
