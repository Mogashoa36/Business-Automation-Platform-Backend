package company.backend.masterdata.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import company.backend.masterdata.entity.MasterDataItem;

/** Data access for {@link MasterDataItem}. */
public interface MasterDataItemRepository extends JpaRepository<MasterDataItem, Long> {

    List<MasterDataItem> findByGroupOrderBySortOrderAscLabelAsc(String group);

    List<MasterDataItem> findByGroupAndActiveTrueOrderBySortOrderAscLabelAsc(String group);

    Optional<MasterDataItem> findByGroupAndCode(String group, String code);

    boolean existsByGroupAndCode(String group, String code);
}