package com.ktk.dukappservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class DeviceTokenDto {
    /** The FCM registration token from the Firebase SDK on the device / in the browser. */
    @NotBlank
    @Size(max = 512)
    private String token;

    /** e.g. "web", "android", "ios". */
    @Size(max = 20)
    private String platform;
}
