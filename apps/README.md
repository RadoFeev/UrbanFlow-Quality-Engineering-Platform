# UrbanFlow Applications

The three independently deployable Spring Boot applications expose responsive,
keyboard-accessible web experiences with stable `data-testid` contracts:

- Customer portal (`3001`): account, address, shipment, tracking, and support flows.
- Courier portal (`3002`): assigned routes, lifecycle updates, evidence, and offline sync.
- Operations dashboard (`3003`): fleet views, governance, incidents, audit, flags, and CSV export.

Start a single application after packaging:

```powershell
.\mvnw.cmd -pl :customer-portal spring-boot:run
```

The portals keep an isolated browser-side demo workspace until the shared service gateway
adapter is enabled in the containerized environment. Browser data can be reset by clearing
site storage for the selected portal origin.
