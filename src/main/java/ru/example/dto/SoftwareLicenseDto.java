package ru.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SoftwareLicenseDto {

    private String licenseId;
    private String softwareName;
    private String owner;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiresAt;

}
