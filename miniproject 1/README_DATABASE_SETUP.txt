SMART MUNICIPAL ROAD MAINTENANCE - DATABASE CHANGE

Changes included:
1. Road records are stored in SQLite database file: smart_road_maintenance.db
2. On application startup, saved roads are loaded into RoadNetwork for Dijkstra.
3. Road IDs are checked for duplicates.
4. Road closures are saved in SQLite and remain closed/open after restarting.
5. Ward Officer enters a Road ID; the application fetches start, end, location, and travel time.
6. Report IDs (DR001, DR002, ...) and submission date/time are generated automatically.
7. Report listing shows the submission date/time and stored road details.

IMPORTANT: Add the SQLite JDBC driver before running the program.

VS Code setup:
1. Download the sqlite-jdbc JAR from the official Xerial SQLite JDBC releases page:
   https://github.com/xerial/sqlite-jdbc/releases
2. Create a folder named lib inside this project folder.
3. Put the downloaded sqlite-jdbc-<version>.jar file inside lib.
4. In VS Code, open the project folder (the folder containing Main.java).
5. If VS Code does not automatically detect the JAR, add it to Java project referenced libraries.
   Alternatively, use a terminal from the project folder:

   Windows PowerShell:
   javac -cp ".;lib\sqlite-jdbc-<version>.jar" *.java
   java -cp ".;lib\sqlite-jdbc-<version>.jar" Main

   Replace <version> with the actual version in the JAR filename.

The database file is created automatically in the working directory the first time the program runs.
Do not delete smart_road_maintenance.db if you want to keep your saved road network.

WARD OFFICER REPORT FLOW:
- Enter an existing Road ID.
- The app fetches the road endpoints and travel time.
- Enter damage type.
- Severity, accident risk, road importance, and traffic rank are still requested because the
  existing repair-priority algorithm uses these values. They cannot be reliably calculated from
  the road ID, endpoints, and travel time alone.
- Report ID and submission timestamp are generated automatically.

SCOPE NOTE:
This update persists the road network and road closure status. Damage reports are still held in
memory, as in the original project, so reports and their timestamps are not retained after the
application closes. Report persistence can be added as a separate next step.
