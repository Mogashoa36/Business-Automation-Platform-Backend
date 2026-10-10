package company.backend.organization.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.audit.AuditAction;
import company.backend.audit.annotation.Audited;
import company.backend.audit.service.AuditService;
import company.backend.exception.ResourceNotFoundException;
import company.backend.organization.dto.OrganizationRequest;
import company.backend.organization.dto.OrganizationResponse;
import company.backend.organization.entity.Organization;
import company.backend.organization.repository.OrganizationRepository;

/**
 * Company profile.
 *
 * <p>A copied project serves exactly one client, so this manages a single record.
 * The values are deliberately data rather than configuration files so that an
 * administrator can set them through the UI.
 */
@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;

    public OrganizationService(OrganizationRepository organizationRepository, AuditService auditService) {
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get() {
        return toResponse(organizationRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> ResourceNotFoundException.of("Organization", 1)));
    }

    /**
     * Creates or replaces the profile.
     *
     * <p>Upsert semantics mean a freshly started deployment behaves sensibly even
     * before the seeder has written a row, instead of returning 404 to an admin who
     * simply has not filled in the company details yet.
     */
    @Audited(module = "organization", action = AuditAction.UPDATE, entityType = "Organization")
    public OrganizationResponse save(OrganizationRequest request) {
        Organization organization =
                organizationRepository.findFirstByOrderByIdAsc().orElseGet(Organization::new);
        OrganizationResponse before = organization.getId() == null ? null : toResponse(organization);

        organization.setName(request.name().trim());
        organization.setLegalName(request.legalName());
        organization.setRegistrationNumber(request.registrationNumber());
        organization.setTaxId(request.taxId());
        organization.setEmail(request.email());
        organization.setPhone(request.phone());
        organization.setWebsite(request.website());
        organization.setLogoUrl(request.logoUrl());
        organization.setAddressLine1(request.addressLine1());
        organization.setAddressLine2(request.addressLine2());
        organization.setCity(request.city());
        organization.setState(request.state());
        organization.setPostalCode(request.postalCode());
        organization.setCountry(request.country());
        organization.setTimezone(request.timezone());
        organization.setCurrency(request.currency());
        organization.setDateFormat(request.dateFormat());

        OrganizationResponse saved = toResponse(organizationRepository.save(organization));
        if (before != null) {
            auditService.recordChange(
                    AuditAction.UPDATE, "organization", "Organization", saved.id(), before, saved, "Company profile updated");
        }
        return saved;
    }

    private static OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getLegalName(),
                organization.getRegistrationNumber(),
                organization.getTaxId(),
                organization.getEmail(),
                organization.getPhone(),
                organization.getWebsite(),
                organization.getLogoUrl(),
                organization.getAddressLine1(),
                organization.getAddressLine2(),
                organization.getCity(),
                organization.getState(),
                organization.getPostalCode(),
                organization.getCountry(),
                organization.getTimezone(),
                organization.getCurrency(),
                organization.getDateFormat(),
                organization.getUpdatedAt());
    }
}