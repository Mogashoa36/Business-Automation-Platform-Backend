package company.backend.masterdata.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import company.backend.common.ApiResponse;
import company.backend.masterdata.dto.MasterDataRequest;
import company.backend.masterdata.dto.MasterDataResponse;
import company.backend.masterdata.service.MasterDataService;
import company.backend.security.Permissions;

import jakarta.validation.Valid;

/** Generic reference lists. */
@RestController
@RequestMapping("/api/master-data")
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.MASTER_DATA_VIEW + "')")
    public ApiResponse<List<MasterDataResponse>> findAll() {
        return ApiResponse.ok(masterDataService.findAll());
    }

    @GetMapping("/{group}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MasterDataResponse>> findByGroup(
            @PathVariable String group,
            @RequestParam(required = false, defaultValue = "true") boolean onlyActive) {
        return ApiResponse.ok(masterDataService.findByGroup(group, onlyActive));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.MASTER_DATA_MANAGE + "')")
    public ApiResponse<MasterDataResponse> create(@Valid @RequestBody MasterDataRequest request) {
        return ApiResponse.created("Item created", masterDataService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.MASTER_DATA_MANAGE + "')")
    public ApiResponse<MasterDataResponse> update(
            @PathVariable Long id, @Valid @RequestBody MasterDataRequest request) {
        return ApiResponse.ok("Item updated", masterDataService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.MASTER_DATA_MANAGE + "')")
    public void delete(@PathVariable Long id) {
        masterDataService.delete(id);
    }
}