## Roadmap and possible changes

#### Automatically cleanup `playerKillStats`

- **Object**: `TotalDeathMessages.playerKillStats`
- **Description**:
  - Players are not removed from the list, which might impact performance, e.g. in the `KillingspreeMessageTask`
- **Solution**:
  - Possibly in `KillingspreeMessageTask`, as we iterate over all entries there
- **Impact**:
  - Performance (at high user count/very long server uptime)
- **Priority**: very low

---

#### Template

- **Object**: `com.example.test`
- **Description**: *insert here*
- **Solution**:
  - *insert*
- **Impact**:
  - *insert*
- **Priority**: *insert here*