package company.backend.organization.dto;

import java.time.Instant;

/** Read model for the company profile. */
public record OrganizationResponse(
        Long id,
        String name,
        String legalName,
        String registrationNumber,
        String taxId,
        String email,
        String phone,
        String website,
        String logoUrl,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        String timezone,
        String currency,
        String dateFormat,
        Instant updatedAt) {
}