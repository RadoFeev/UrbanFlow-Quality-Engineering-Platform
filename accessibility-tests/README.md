# Accessibility suite

This module runs axe-core through Selenium against the three production portal asset sets. Fifteen
responsive page combinations enforce the WCAG 2 A/AA and WCAG 2.1 A/AA rule families and fail on
critical or serious violations. The complete axe violation evidence is included in assertion output.

Run locally with Chrome:

```powershell
.\mvnw.cmd -pl :accessibility-tests -am -Dgroups=accessibility test
```

Automated analysis complements the keyboard, focus, screen-reader, zoom, reflow, contrast, and
reduced-motion manual checklist in `docs/ACCESSIBILITY_TESTING.md`.
