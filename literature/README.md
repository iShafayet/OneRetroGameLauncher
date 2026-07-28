# Literature — data & storage

Technical notes for how ORGL persists and owns data. Product/UX copy lives in
[app-functionality-summary.md](./app-functionality-summary.md).

| Doc | Covers |
|-----|--------|
| [data-overview.md](./data-overview.md) | Mental model: what lives where, trust boundaries |
| [database.md](./database.md) | Room `orgl.db` schema, tables, FKs, migrations |
| [app-storage.md](./app-storage.md) | DataStores, backup rules, internal app files |
| [orgl-directory.md](./orgl-directory.md) | ORGL folder contract, SAF trees (ROMs / ORGL / ES-DE), media layout |
| [assumptions-and-limitations.md](./assumptions-and-limitations.md) | Deliberate product constraints, permanent naming quirks, channel/package assumptions |
| [release-foss.md](./release-foss.md) | FOSS release checklist only |
| [release-play.md](./release-play.md) | Play release workflow and guardrails |

**Source of truth:** Kotlin under `app/src/main/java/.../data/` and `library/`.
Update these docs when the schema, prefs keys, or folder contract change.
