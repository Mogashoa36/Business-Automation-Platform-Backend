package company.backend.security;

/**
 * Role codes seeded by the platform.
 *
 * <p>These four cover the common organisational shapes (full access, supervision,
 * day-to-day work, read-only) and are a starting point rather than a constraint.
 * A client project is free to rename, extend or ignore them, which is why nothing
 * outside the seeder and the self-protection checks treats these codes as special.
 */
public final class SystemRoles {

    private SystemRoles() {
    }

    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String EMPLOYEE = "EMPLOYEE";
    public static final String VIEWER = "VIEWER";
}