package io.urbanflow.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Persisted authenticated actor with lockout and optimistic version state. */
@Entity
@Table(name = "user_accounts")
public class UserAccount {

  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AccountStatus status;

  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  /** Constructor reserved for persistence. */
  protected UserAccount() {}

  /** Registers an active account with normalized email and customer role. */
  public static UserAccount register(
      UUID id, String email, String displayName, String passwordHash, Instant now) {
    UserAccount account = new UserAccount();
    account.id = Objects.requireNonNull(id, "id must not be null");
    account.email = normalizeEmail(email);
    account.displayName = requireText(displayName, "displayName");
    account.passwordHash = requireText(passwordHash, "passwordHash");
    account.role = Role.CUSTOMER;
    account.status = AccountStatus.ACTIVE;
    account.createdAt = Objects.requireNonNull(now, "now must not be null");
    account.updatedAt = now;
    return account;
  }

  /** Records a failed login and activates a temporary lock after the configured threshold. */
  public void recordFailedLogin(int maximumAttempts, Instant lockedUntil, Instant now) {
    if (status == AccountStatus.DISABLED) {
      return;
    }
    failedLoginAttempts++;
    if (failedLoginAttempts >= maximumAttempts) {
      status = AccountStatus.LOCKED;
      this.lockedUntil = Objects.requireNonNull(lockedUntil, "lockedUntil must not be null");
    }
    updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Clears failures and an expired temporary lock after successful credential validation. */
  public void recordSuccessfulLogin(Instant now) {
    if (status == AccountStatus.LOCKED && lockedUntil != null && !now.isBefore(lockedUntil)) {
      status = AccountStatus.ACTIVE;
    }
    failedLoginAttempts = 0;
    lockedUntil = null;
    updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Returns whether login is permitted at the supplied time. */
  public boolean canAuthenticate(Instant now) {
    return status == AccountStatus.ACTIVE
        || (status == AccountStatus.LOCKED && lockedUntil != null && !now.isBefore(lockedUntil));
  }

  /** Changes the password and clears any temporary lock state. */
  public void changePassword(String passwordHash, Instant now) {
    this.passwordHash = requireText(passwordHash, "passwordHash");
    if (status != AccountStatus.DISABLED) {
      status = AccountStatus.ACTIVE;
    }
    failedLoginAttempts = 0;
    lockedUntil = null;
    updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Changes the assigned role. */
  public void changeRole(Role role, Instant now) {
    this.role = Objects.requireNonNull(role, "role must not be null");
    updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  /** Returns the stable user identifier. */
  public UUID id() {
    return id;
  }

  /** Returns the normalized email. */
  public String email() {
    return email;
  }

  /** Returns the user-facing name. */
  public String displayName() {
    return displayName;
  }

  /** Returns the adaptive credential hash for verification only. */
  public String passwordHash() {
    return passwordHash;
  }

  /** Returns the current authorization role. */
  public Role role() {
    return role;
  }

  /** Returns the administrative account state. */
  public AccountStatus status() {
    return status;
  }

  /** Returns the consecutive failed-login count. */
  public int failedLoginAttempts() {
    return failedLoginAttempts;
  }

  /** Returns when a temporary lock expires. */
  public Instant lockedUntil() {
    return lockedUntil;
  }

  /** Returns the creation timestamp. */
  public Instant createdAt() {
    return createdAt;
  }

  /** Returns the most recent mutation timestamp. */
  public Instant updatedAt() {
    return updatedAt;
  }

  /** Returns the optimistic persistence version. */
  public long version() {
    return version;
  }

  /** Produces the repository-wide canonical email form. */
  public static String normalizeEmail(String email) {
    return requireText(email, "email").toLowerCase(Locale.ROOT);
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.trim();
  }
}
