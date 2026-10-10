package company.backend.workflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import company.backend.workflow.entity.WorkflowInstanceStep;

/** Data access for {@link WorkflowInstanceStep}. */
public interface WorkflowInstanceStepRepository extends JpaRepository<WorkflowInstanceStep, Long> {

    List<WorkflowInstanceStep> findByInstanceIdOrderByStepOrderAsc(Long instanceId);
}