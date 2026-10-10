package company.backend.dashboard.dto;

import java.util.List;

/** Aggregated landing-page payload. Every figure is computed live from the database. */
public record DashboardResponse(
        List<SummaryCard> summary,
        List<ActivityItem> recentActivity,
        List<WorkItem> myTasks,
        List<WorkItem> pendingApprovals,
        List<WorkItem> pendingWorkflows,
        List<StatusCount> taskStatusBreakdown,
        List<StatusCount> approvalStatusBreakdown) {

    /**
     * A headline number.
     *
     * @param trend percentage change against the previous period, or null when
     *              there is no comparable data
     */
    public record SummaryCard(
            String key, String label, long value, String linkUrl, Double trend, String icon) {
    }

    /** A recent audit entry, rendered as an activity feed. */
    public record ActivityItem(Long id, String action, String module, String description, String username, String createdAt) {
    }

    /** A task, approval or workflow shown in a dashboard list. */
    public record WorkItem(Long id, String reference, String title, String status, String subtitle, String linkUrl) {
    }

    /** A count for one status value, used to draw the distribution bars. */
    public record StatusCount(String status, long count) {
    }
}