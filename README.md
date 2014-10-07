TestServer is a simple application based on NIO client server framework called «Netty». 
What it can?

1. On «../hello» request it responds by «Hello World!» string in 10 sec.
2. On «../redirect?url=<url>» — redirects to asked url.
3. On «../status» — shows the status page, which consists of:
  3.1 total amount of served requests
  3.2 current amount of opened connections
  3.3 amount of unique requests (one per IP)
  3.4 «request-to-IP» table (IP, amount of requests, time of the last one)
  3.5 «redirect» table (url, amount of redirects)
  3.6 «log of last 16» table (IP, URI, Timestamp, sent_bytes, received_bytes, speed) 
