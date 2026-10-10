package company.backend.workflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import company.backend.workflow.entity.WorkflowDefinition;

/** Data access for {@link WorkflowDefinition}. */
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    @EntityGraph(attributePaths = "steps")
    List<WorkflowDefinition> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"steps", "steps.assigneeRole"})
    Optional<WorkflowDefinition> findWithStepsById(Long id);

    @EntityGraph(attributePaths = {"steps", "steps.assigneeRole"})
    Optional<WorkflowDefinition> findWithStepsByCode(String code);

    boolean existsByCode(String code);
}