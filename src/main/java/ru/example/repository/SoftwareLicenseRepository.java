package ru.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.example.entity.SoftwareLicenseEntity;

import java.util.Optional;

public interface SoftwareLicenseRepository extends JpaRepository<SoftwareLicenseEntity, Long> {

    Optional<SoftwareLicenseEntity> findByLicenseId(String licenseId);
}
