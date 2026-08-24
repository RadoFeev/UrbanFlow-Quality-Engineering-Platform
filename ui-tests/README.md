# Selenium UI suite

The suite drives the customer, courier, and operations portals through stable `data-testid` and CSS
locators. A deterministic embedded HTTP fixture serves the real portal assets for local runs; setting
the three portal URL properties targets deployed applications instead. Every invocation owns an
independent browser, storage namespace, correlation ID, trace ID, and failure-evidence lifecycle.

```powershell
.\mvnw.cmd -pl :ui-tests -am -Dgroups=ui -Dbrowser=edge -Dheadless=true clean test
.\mvnw.cmd -pl :ui-tests -am -Dgroups=ui -Dbrowser=firefox -Dheadless=true clean test
.\mvnw.cmd -pl :ui-tests -am -Dgroups=ui -Dremote.url=http://localhost:4444 clean test
```

The suite uses Selenium Manager's offline cache by default for deterministic CI startup. On a new
workstation, add `-Dselenium.offline=false` to one local run so the matching browser driver can be
resolved and cached.

Set `-Dvideo=true` for compatible Grid nodes. Chromium sessions collect browser and performance logs;
all failures attach a screenshot, page source, browser log, correlation ID, and trace ID to Allure.
