# Intended Deviations

These deviations are locked before crawl and suppress false parity failures:

| ID | Area | MySD decision | Fit handling |
|---|---|---|---|
| DEV-001 | Identity/IP | Original title, world, characters, units, enemies, buildings, lore | mask creative regions; compare structure |
| DEV-002 | Visual assets | Original art, icons, effects, typography choices, palette | mask IP/pixel regions; keep bounds/layout contract |
| DEV-003 | Audio | Original music, SFX, voices | behavioral event presence only |
| DEV-004 | Copy | Original UI and narrative prose | compare semantic role and bounds, not text pixels |
| DEV-005 | Balance | Original numeric values preserving accepted pacing relationships | compare timings/tier shape, not exact numbers |
| DEV-006 | Rewarded ads | deterministic local adapter, no real ad | compare affordance/guard/result shape |
| DEV-007 | IAP | local catalog/result fake, no payment | compare surface/guards; no transaction |
| DEV-008 | Arena | local service-shaped state, no network match | mark network path service_adapter/blocked |
| DEV-009 | Accessibility | minimum 48 dp targets and scalable text | intentional bounds/hit-area deviation if needed |

Additional deviations found during crawl require a human decision at Gate 1 or Gate 2.
