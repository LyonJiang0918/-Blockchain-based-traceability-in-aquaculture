const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 8082;
const DIRECTORY = path.join(__dirname);
// Backend runs on 7777 (see backend/src/main/resources/application.yml).
const API_TARGET = { hostname: '127.0.0.1', port: 7777 };

const mimeTypes = {
    '.html': 'text/html',
    '.js': 'text/javascript',
    '.css': 'text/css',
    '.json': 'application/json',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml'
};

function proxyToBackend(req, res) {
    const headers = { ...req.headers };
    headers.host = `${API_TARGET.hostname}:${API_TARGET.port}`;

    const proxyReq = http.request(
        {
            hostname: API_TARGET.hostname,
            port: API_TARGET.port,
            path: req.url,
            method: req.method,
            headers
        },
        (proxyRes) => {
            res.writeHead(proxyRes.statusCode, proxyRes.headers);
            proxyRes.pipe(res);
        }
    );

    proxyReq.on('error', (err) => {
        res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(
            JSON.stringify({
                success: false,
                message: `无法连接后端（请确认已在 ${API_TARGET.port} 端口启动 Spring Boot）: ${err.message}`
            }),
            'utf-8'
        );
    });

    req.pipe(proxyReq);
}

const server = http.createServer((req, res) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.url}`);

    if (req.url.startsWith('/api')) {
        proxyToBackend(req, res);
        return;
    }

    let filePath = req.url === '/' ? '/index.html' : req.url.split('?')[0];
    filePath = path.join(DIRECTORY, filePath);

    const extname = String(path.extname(filePath)).toLowerCase();
    const contentType = mimeTypes[extname] || 'application/octet-stream';

    fs.readFile(filePath, (error, content) => {
        if (error) {
            if (error.code === 'ENOENT') {
                res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' });
                res.end('<h1>404 - 文件未找到</h1>', 'utf-8');
            } else {
                res.writeHead(500);
                res.end('服务器错误: ' + error.code);
            }
        } else {
            res.writeHead(200, { 'Content-Type': contentType });
            res.end(content, 'utf-8');
        }
    });
});

server.listen(PORT, () => {
    console.log(`用户端服务已启动: http://localhost:${PORT}`);
    console.log(`API 将转发至: http://${API_TARGET.hostname}:${API_TARGET.port}`);
    console.log(`当前目录: ${DIRECTORY}`);
});
