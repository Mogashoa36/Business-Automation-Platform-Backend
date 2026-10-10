package company.backend.settings.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import company.backend.settings.entity.SystemSetting;

/** Data access for {@link SystemSetting}. */
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findByKey(String key);

    List<SystemSetting> findByGroupOrderByKeyAsc(String group);

    List<SystemSetting> findAllByOrderByGroupAscKeyAsc();
}