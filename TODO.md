## Roadmap and possible changes

### Automatically cleanup `playerKillStats`

- **Object**: `TotalDeathMessages.playerKillStats`
- **Description**:
  - Players are not removed from the list, which might impact performance, e.g. in the `KillingspreeMessageTask`
- **Solution**:
  - Possibly in `KillingspreeMessageTask`, as we iterate over all entries there
- **Impact**:
  - Performance (at high user count/very long server uptime)
- **Priority**: very low

### Refactor `onEntityDeath`
- **Object**: `EntityDeathListener.onEntityDeath`
- **Description**:
  - Handler for Entity deaths
  - Wayyyy too long to be understandable, giant nested if-else abomination
- **Solution**:
  - Replace with some kind of plug-and-play Handler system, where each death type is called from a central place
- **Impact**:
  - Code Readability
  - Maintainability
- **Priority**: high

### Remove BungeeCord chat components
- **Object**: *everywhere*
- **Description**:
  - BungeeCord chat system was deprecated in Paper and replaced by Adventure
- **Solution**:
  - Replace all BungeeCord chat component usages with Adventure
- **Impact**:
  - Maintainability
- **Priority**: medium

---
### Template

- **Object**: `com.example.test`
- **Description**: *insert here*
- **Solution**:
  - *insert*
- **Impact**:
  - *insert*
- **Priority**: *insert here*