# IslandVault – Plugin-Anleitung

**Version:** 1.21.4\
**Kompatibel mit:** Bukkit, Spigot, Paper\
**Zweck:** Virtuelles Lager mit unbegrenztem Speicherplatz pro freigeschaltetem Item.\
**Aktuelle Plugin Version:** 1.0

## Funktionen

*   Virtuelles Insellager: Spieler können ein virtuelles Lager öffnen, das unbegrenzten Platz für bestimmte Items bietet.
*   Unbegrenzter Speicher: Für jedes freigeschaltete Item gibt es einen eigenen Slot, der eine unbegrenzte Menge dieses Items speichern kann.
*   Einschränkungen: Nur Items ohne NBT-Daten (z. B. keine verzauberten oder benannten Items) können eingelagert werden.
*   GUI: Spieler öffnen das Lager über die Befehle `/lager` oder `/insellager`.
*   Rechteverwaltung: Über Permissions kann gesteuert werden, welche Spieler welche Items lagern dürfen. Zur Vereinfachung können Items in Gruppen zusammengefasst werden (z. B. für Ränge).

## Befehle

| Befehl | Beschreibung | Benötigte Permission |
| --- | --- | --- |
| `/lager` | Öffnet die GUI des Insellagers | `islandvault.canvaultopen` |
| `/insellager` | Alias für `/lager` | `islandvault.canvaultopen` |

_Nur Spieler mit der Permission `islandvault.canvaultopen` können das Lager öffnen._

## Permissions im Detail

*   `islandvault.command.openGUI`  
    Erlaubt das Öffnen der Insellager-GUI. Ohne diese Permission kann der Spieler das Lager nicht benutzen.
*   `islandvaults.vault.<Material>`  
    Erlaubt das Lagern und Entnehmen eines bestimmten Materials (z. B. `islandvaults.vault.DIAMOND`).  
    Der Platzhalter `<Material>` entspricht dem Materialnamen in Großbuchstaben (Minecraft-Materialnamen).
*   `islandvaults.groups.<Gruppenname>`  
    Erlaubt das Lagern und Entnehmen aller Items, die in einer vordefinierten Gruppe zusammengefasst sind.  
    Diese Gruppen sind in der `config.yml` unter `permission_groups` definiert, z.B. eine Gruppe `vip` mit bestimmten Items. So müssen nicht alle Items einzeln als Permission vergeben werden, was vor allem bei Rängen praktisch ist.
*   `islandvault.collectradius.<Radius>` Der Radius für das automatische Sammeln bei dem die Spieler die gedroppten Items automatisch in ihr Insellager erhalten, sofern das Item freigeschaltet ist. Ist in der `config.yml` unter `auto-collect-radius` definiert.
## config.yml erklärt

```
auto-collect-radius:
- 5
- 10
- 20

blacklist: [spawn_egg, head]

permission_groups:
  test: [REDSTONE_TORCH, REDSTONE_BLOCK, REDSTONE]

messages:
  itemLore:
    - "§9Menge: §r%amount%"
    - ""
    - "§eLinksklick: §b%maxstacksize%x"
    - "§eRechtsklick: §b1x"
  menuTitle: "Dein Insellager"
  noPlayer: "§cDieser Befehl kann nur vom Spieler ausgeführt werden!"
  noPermission: "§cDu hast nicht die Berechtigung diesen Befehl zu nutzen!"
  firstPage: "§aErste Seite"
  previousPage: "§aVorherige Seite"
  nextPage: "§aNächste Seite"
  lastPage: "§aLetzte Seite"
  close: "§4Schließen"
```

**auto-collect-radius:** Der per Permission einstellbare Radius für das automatische Sammeln ins Insellager.\
**blacklist:** Items, die niemals eingelagert werden können (z.B. Spawn-Eier, Köpfe).  
**permission\_groups:** Gruppen mit mehreren Items, die per eine einzige Permission freigeschaltet werden können (z.B. Ränge).  
**messages:** Texte für GUI und Feedback, z.B. Tooltips für Items. Farbcodes sind nutzbar.

## Beispiel für eine Permission-Gruppe "vip"

```
permission_groups:
  vip: [REDSTONE, IRON_INGOT, DIAMOND]
```

Spieler mit der Permission `islandvaults.groups.vip` können diese Items lagern.  
Zusätzlich benötigen sie `islandvault.canvaultopen` zum Öffnen.

## GUI Steuerung

*   **Linksklick:** Maximalen Stack aus dem Lager entnehmen
*   **Rechtsklick:** 1 Item entnehmen

## Wichtige Hinweise

*   Items mit NBT-Daten (benannte Items, Tränke, verzauberte Items, etc.) können **nicht** eingelagert werden.
*   Änderungen an der `config.yml` erfordern einen Server-Neustart oder Plugin-Reload.
*   Rechtevergabe ist entscheidend für Zugriff auf Items.
