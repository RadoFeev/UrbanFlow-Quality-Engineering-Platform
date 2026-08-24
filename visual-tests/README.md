# Visual regression suite

Fifteen versioned PNG baselines cover the customer, courier, and operations portals at mobile,
tablet, laptop, desktop, and wide-screen viewport sizes. The comparison tolerates a per-channel
rendering delta of 12 and fails when more than 0.1% of pixels differ. Failures write `actual` and
high-contrast `diff` evidence under `target/visual-diffs`.

```powershell
# Compare with committed baselines
.\mvnw.cmd -pl :visual-tests -am -Dgroups=visual test

# Intentional design change: inspect output, then regenerate baselines
.\mvnw.cmd -pl :visual-tests -am -Dgroups=visual -Dvisual.update=true test
```

Baseline updates require review alongside the corresponding UI change; CI never enables update mode.
