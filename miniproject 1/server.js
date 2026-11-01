// Optional lightweight static server for Front-End on port 5501
const http = require("http");
const fs = require("fs");
const path = require("path");

const PORT = 5501;
const BASE_DIR = __dirname;
const FRONTEND_DIR = path.join(BASE_DIR, "Front-End");

const MIME_TYPES = {
    ".html": "text/html; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".js": "application/javascript; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".gif": "image/gif",
    ".svg": "image/svg+xml",
    ".ico": "image/x-icon"
};

const server = http.createServer((req, res) => {
    // Add CORS headers
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Methods", "GET, POST, PATCH, OPTIONS");
    res.setHeader("Access-Control-Allow-Headers", "Content-Type");

    if (req.method === "OPTIONS") {
        res.writeHead(204);
        res.end();
        return;
    }

    const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
    let reqPath = decodeURIComponent(parsedUrl.pathname);

    if (reqPath === "/" || reqPath === "/Front-End" || reqPath === "/Front-End/") {
        reqPath = "/Front-End/index.html";
    }

    // Try resolving in project root first, then in Front-End folder
    let filePath = path.join(BASE_DIR, reqPath);
    if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
        filePath = path.join(FRONTEND_DIR, reqPath);
    }
    if ((!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) && reqPath.startsWith("/Front-End/")) {
        filePath = path.join(FRONTEND_DIR, reqPath.replace(/^\/Front-End\//, ""));
    }

    if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
        res.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
        res.end("404 Not Found");
        return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || "application/octet-stream";

    res.writeHead(200, { "Content-Type": contentType });
    fs.createReadStream(filePath).pipe(res);
});

server.listen(PORT, "127.0.0.1", () => {
    console.log(`Frontend static server running at http://localhost:${PORT}/Front-End/`);
    console.log(`Also accessible at http://localhost:${PORT}/`);
});
