package ru.example.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SoftwareLicenseDto {

    private String licenseId;
    private String softwareName;
    private String owner;
    private LocalDate expiresAt;

}
