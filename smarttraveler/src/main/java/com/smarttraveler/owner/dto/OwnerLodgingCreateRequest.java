package com.smarttraveler.owner.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

public record OwnerLodgingCreateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 30) String lodgingType,
        @NotBlank @Size(max = 300) String address,
        @NotNull @DecimalMin("-90") @DecimalMax("90")
        @Digits(integer = 3, fraction = 7) BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180")
        @Digits(integer = 3, fraction = 7) BigDecimal longitude,
        @NotNull LocalTime checkinTime,
        @NotNull LocalTime checkoutTime
) {}

//숙소 등록 화면에서 서버로 보낼 값과 입력 제한을 정하는 클래스(참고용)