package ru.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "software_license")
public class SoftwareLicenseEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String licenseId;

    private String softwareName;

    private String owner;

    private LocalDate expiresAt;

}
