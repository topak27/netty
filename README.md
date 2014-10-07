TestServer is a simple application based on NIO client server framework called «Netty». 
What it can?

1. On «../hello» request it responds by «Hello World!» string in 10 sec.
2. On «../redirect?url=<url>» — redirects to asked url.
3. On «../status» — shows the status page, which consists of:
  — total amount of served requests
  — current amount of opened connections
  — amount of unique requests (one per IP)
  — «request-to-IP» table (IP, amount of requests, time of the last one)
  — «redirect» table (url, amount of redirects)
  — «log of last 16» table (IP, URI, Timestamp, sent_bytes, received_bytes, speed) 
