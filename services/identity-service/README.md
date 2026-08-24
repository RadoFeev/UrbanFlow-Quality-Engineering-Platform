# Identity Service

Identity Service owns UrbanFlow user credentials, account lockout, role assignments, short-lived
signed access tokens, rotating refresh sessions, and single-use password resets.

## Security properties

- Passwords are bcrypt encoded and must satisfy the central four-character-class policy.
- Access credentials use HS256 with explicit issuer, audience, ID, issue, and expiry claims.
- Refresh and reset credentials are generated with 256 bits of entropy and only SHA-256 hashes are
  persisted.
- Refresh exchange rotates and revokes the previous credential; password reset revokes every live
  session for the account.
- Login errors do not reveal whether an email exists. Repeated failures activate a temporary lock.
- Role changes require the `ROLE_MANAGE` permission and an `If-Match` version entity tag.

The service requires `IDENTITY_DATABASE_USER`, `IDENTITY_DATABASE_PASSWORD`, and a minimum
32-character `IDENTITY_SIGNING_SECRET`. See the root README for complete platform startup commands.
