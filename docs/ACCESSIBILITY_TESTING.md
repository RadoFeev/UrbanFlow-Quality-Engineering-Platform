# Accessibility Testing

## Standard and scope

The automated gate targets WCAG 2.2 Level AA for login, registration, create shipment, tracking, courier dashboard, and operations dashboard. Additional checks cover reusable components, error states, dialogs, tables, pagination, language changes, and responsive layouts.

## Automation

axe-core runs through Selenium after the page and relevant dynamic state become stable. Rules are tagged to WCAG 2.2 A/AA where supported. Each violation attachment includes description, impact/severity, help text/link, CSS selectors, affected HTML snippet, and remediation guidance.

Critical violations block the accessibility workflow. Serious violations block gated critical pages; other findings are tracked and trended. Exclusions must identify the exact rule/node, rationale, owner, and expiry.

## Manual complement

Automation does not prove accessibility. Milestone/release review includes keyboard-only navigation, visible focus, logical focus order, skip links, screen-reader names/roles/states, live-region announcements, zoom/reflow, color/contrast review, error recovery, language metadata, and reduced-motion behavior.

## Test stability

Tests wait for semantic state rather than animation duration, scan one well-defined context, attach DOM state on failure, and use stable selectors. Third-party widgets are included unless a documented external limitation has an active remediation plan.
