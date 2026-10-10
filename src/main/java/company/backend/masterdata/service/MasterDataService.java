package company.backend.masterdata.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.audit.AuditAction;
import company.backend.audit.annotation.Audited;
import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;
import company.backend.masterdata.dto.MasterDataRequest;
import company.backend.masterdata.dto.MasterDataResponse;
import company.backend.masterdata.entity.MasterDataItem;
import company.backend.masterdata.repository.MasterDataItemRepository;

/** Lightweight reference lists: categories, types, locations and similar. */
@Service
@Transactional
public class MasterDataService {

    private final MasterDataItemRepository repository;

    public MasterDataService(MasterDataItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MasterDataResponse> findByGroup(String group, boolean onlyActive) {
        String normalised = group.toUpperCase(Locale.ROOT);
        List<MasterDataItem> items = onlyActive
                ? repository.findByGroupAndActiveTrueOrderBySortOrderAscLabelAsc(normalised)
                : repository.findByGroupOrderBySortOrderAscLabelAsc(normalised);
        return items.stream().map(MasterDataService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MasterDataResponse> findAll() {
        return repository.findAll().stream().map(MasterDataService::toResponse).toList();
    }

    @Audited(module = "masterdata", action = AuditAction.CREATE, entityType = "MasterDataItem")
    public MasterDataResponse create(MasterDataRequest request) {
        String group = request.group().toUpperCase(Locale.ROOT);
        String code = request.code().toUpperCase(Locale.ROOT);
        if (repository.existsByGroupAndCode(group, code)) {
            throw new BusinessException("'" + code + "' already exists in " + group);
        }
        MasterDataItem item = new MasterDataItem();
        item.setGroup(group);
        item.setCode(code);
        item.setLabel(request.label().trim());
        item.setDescription(request.description());
        item.setSortOrder(request.sortOrderOrDefault());
        item.setActive(request.isActiveOrDefault());
        return toResponse(repository.save(item));
    }

    @Audited(module = "masterdata", action = AuditAction.UPDATE, entityType = "MasterDataItem")
    public MasterDataResponse update(Long id, MasterDataRequest request) {
        MasterDataItem item = requireItem(id);
        assertNotSystemManaged(item);

        item.setLabel(request.label().trim());
        item.setDescription(request.description());
        item.setSortOrder(request.sortOrderOrDefault());
        if (request.active() != null) {
            item.setActive(request.active());
        }
        return toResponse(repository.save(item));
    }

    @Audited(module = "masterdata", action = AuditAction.DELETE, entityType = "MasterDataItem")
    public void delete(Long id) {
        MasterDataItem item = requireItem(id);
        assertNotSystemManaged(item);
        repository.delete(item);
    }

    private void assertNotSystemManaged(MasterDataItem item) {
        if (item.isSystemManaged()) {
            throw new BusinessException("'" + item.getCode() + "' is a platform value and cannot be changed");
        }
    }

    @Transactional(readOnly = true)
    public MasterDataItem requireItem(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Master data item", id));
    }

    static MasterDataResponse toResponse(MasterDataItem item) {
        return new MasterDataResponse(
                item.getId(),
                item.getGroup(),
                item.getCode(),
                item.getLabel(),
                item.getDescription(),
                item.getSortOrder(),
                item.isActive(),
                item.isSystemManaged(),
                item.getUpdatedAt());
    }
}