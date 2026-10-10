package company.backend.settings.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import company.backend.config.PlatformProperties;
import company.backend.document.service.DocumentService;
import company.backend.exception.BusinessException;
import company.backend.exception.ResourceNotFoundException;
import company.backend.settings.entity.SystemSetting;
import company.backend.settings.repository.SystemSettingRepository;

/**
 * Runtime-editable platform settings.
 *
 * <p>Combines two sources on purpose: values an administrator may change freely
 * live in the database, while branding, storage and security values come from
 * configuration and are exposed read-only. Mixing the two would give operators the
 * impression they can weaken security from the UI.
 */
@Service
@Transactional
public class SettingsService {

    private final SystemSettingRepository settingRepository;
    private final PlatformProperties platformProperties;
    private final DocumentService documentService;

    public SettingsService(
            SystemSettingRepository settingRepository,
            PlatformProperties platformProperties,
            DocumentService documentService) {
        this.settingRepository = settingRepository;
        this.platformProperties = platformProperties;
        this.documentService = documentService;
    }

    @Transactional(readOnly = true)
    public List<SystemSetting> findAll() {
        return settingRepository.findAllByOrderByGroupAscKeyAsc();
    }

    @Transactional(readOnly = true)
    public List<SystemSetting> findByGroup(String group) {
        return settingRepository.findByGroupOrderByKeyAsc(group);
    }

    @Transactional(readOnly = true)
    public SystemSetting findByKey(String key) {
        return settingRepository
                .findByKey(key)
                .orElseThrow(() -> ResourceNotFoundException.of("Setting", key));
    }

    public SystemSetting update(String key, String value) {
        SystemSetting setting = settingRepository
                .findByKey(key)
                .orElseThrow(() -> ResourceNotFoundException.of("Setting", key));
        if (!setting.isEditable()) {
            throw new BusinessException("Setting '" + key + "' is managed by configuration and cannot be edited here");
        }
        setting.setValue(value);
        return settingRepository.save(setting);
    }

    /** Branding and runtime information for the Angular shell. */
    @Transactional(readOnly = true)
    public Map<String, Object> platformInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", platformProperties.getName());
        info.put("shortName", platformProperties.getShortName());
        info.put("version", platformProperties.getVersion());
        info.put("supportEmail", platformProperties.getSupportEmail());
        info.put("storageBackend", documentService.describeStorage());
        return info;
    }
}