package company.backend.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for editing the company profile. */
public record OrganizationRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String legalName,
        @Size(max = 100) String registrationNumber,
        @Size(max = 100) String taxId,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String phone,
        @Size(max = 255) String website,
        @Size(max = 500) String logoUrl,
        @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @Size(max = 100) String city,
        @Size(max = 100) String state,
        @Size(max = 30) String postalCode,
        @Size(max = 100) String country,
        @Size(max = 64) String timezone,
        @Size(max = 10) String currency,
        @Size(max = 32) String dateFormat) {
}