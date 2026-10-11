SMART MUNICIPAL ROAD MAINTENANCE - DATABASE CHANGE

Changes included:
1. Road records are stored in SQLite database file: smart_road_maintenance.db
2. On application startup, saved roads are loaded into RoadNetwork for Dijkstra.
3. Road IDs are checked for duplicates.
4. Road closures are saved in SQLite and remain closed/open after restarting.
5. Ward Officer enters a Road ID; the application fetches start, end, location, and travel time.
6. Report IDs (DR001, DR002, ...) and submission date/time are generated automatically.
7. Report listing shows the submission date/time and stored road details.
8. A local HTTP API serves the SQLite road network to the browser frontend.

IMPORTANT: Add the SQLite JDBC driver before running the program.

VS Code setup:
1. Download the sqlite-jdbc JAR from the official Xerial SQLite JDBC releases page:
   https://github.com/xerial/sqlite-jdbc/releases
2. Create a folder named lib inside this project folder.
3. Put the downloaded sqlite-jdbc-<version>.jar file inside lib.
4. In VS Code, open the project folder (the folder containing Main.java).
5. If VS Code does not automatically detect the JAR, add it to Java project referenced libraries.
   Alternatively, use a terminal from the project folder:

   Windows PowerShell (run from the folder containing Main.java):
      javac -cp ".;lib\sqlite-jdbc-<version>.jar" (Get-ChildItem -Filter *.java | ForEach-Object { $_.FullName })
      java -cp ".;lib\sqlite-jdbc-<version>.jar" Main

   Linux/macOS (run from the folder containing Main.java):
      javac -cp ".:lib/sqlite-jdbc-<version>.jar" *.java
      java -cp ".:lib/sqlite-jdbc-<version>.jar" Main

   Replace <version> with the actual version in the JAR filename.

The database file is created automatically in the working directory the first time the program runs.
Do not delete smart_road_maintenance.db if you want to keep your saved road network.

FRONTEND ROAD NETWORK CONNECTION:
- Start Main with the SQLite JDBC JAR on its classpath. Main starts the Road API at
   http://127.0.0.1:8080/api/roads while keeping the console application available.
- Keep that Java process running, then open Front-End/index.html with VS Code Live Server.
   Alternatively, run `python3 -m http.server 5501` from the project folder and open
   http://127.0.0.1:5501/Front-End/ in a browser.
- The frontend loads roads, reports, report road choices, and routes from the SQLite-backed API.
   Engineering can add roads and close/reopen them; Ward Officers can submit damage reports;
   report status changes are also saved. The route finder uses Java's Dijkstra implementation.
- The Road Network page's Refresh button reloads road changes. Opening the Reports page reloads
   saved reports from SQLite.
- If the frontend says it cannot load roads, verify the Java process is running and that the
   SQLite JDBC JAR is included in its runtime classpath.
- The API listens on 127.0.0.1:8080 and exposes:
   - GET /api/roads to list roads.
   - POST /api/roads with form fields id, from, to, and time to add a road.
   - PATCH /api/roads/{roadId}/status?closed=true|false to save closure status.
   - GET /api/routes?start={location}&destination={location} to calculate a route.
   - GET /api/reports to list saved damage reports.
   - POST /api/reports with roadId, damageType, severity, risk, importance, and traffic to submit.
   - PATCH /api/reports/{reportId}/status?status=Pending|Active|Completed to save report status.
- Demo accounts and maintenance-team data remain frontend/demo data; damage reports and roads
   are persisted in SQLite.

WARD OFFICER REPORT FLOW:
- Enter an existing Road ID.
- The app fetches the road endpoints and travel time.
- Enter damage type.
- Severity, accident risk, road importance, and traffic rank are still requested because the
  existing repair-priority algorithm uses these values. They cannot be reliably calculated from
  the road ID, endpoints, and travel time alone.
- Report ID and submission timestamp are generated automatically.

PERSISTENCE NOTE:
Road segments, closure status, damage report details, report statuses, and report submission
timestamps are retained in SQLite after the application closes.
