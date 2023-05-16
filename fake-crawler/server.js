const http = require("http");

const hostname = "127.0.0.1";
const port = 3000;

// En test fake for ytelsestesting av "start crawling". Brukes i no/uutilsynet/testlab2testing/StartCrawlingPerformanceTest.kt.
const server = http.createServer((req, res) => {
    const queryIndex = req.url.indexOf("?");
    const path = queryIndex === -1 ? req.url : req.url.slice(0, queryIndex);
    if (req.method === "POST" && path === "/crawler") {
        req.on("data", (chunk) => {
            // Ignore any data given in the body
        });
        req.on("end", () => {
            const response = {statusQueryGetUri: "http://status.uri"};
            const delay = Math.random() // Random delay between 0 and 1 second
            setTimeout(() => {
                res.statusCode = 200;
                res.setHeader("Content-Type", "application/json");
                res.end(JSON.stringify(response));
            }, delay * 1000);
        });
    } else {
        res.statusCode = 404;
        res.setHeader("Content-Type", "text/plain");
        res.end("Not found");
    }
});

server.listen(port, hostname, () => {
    console.log(`Server running at http://${hostname}:${port}/`);
});
