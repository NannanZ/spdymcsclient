SPDY MCS Client
=============

It's a SPDY based client of renren's mcs(Mobile Client Service) powered by [OkHttp Java library](https://github.com/square/okhttp)
<br>Bellow are some relevant points you might concern for this project:
- Regarding renren's mcs (Mobile Client Service), you can refer to this site [here](http://wiki.mobile.renren.com/en/).
- Regarding the SPDY relevant leaning stuff, you can get the info [here](http://dev.chromium.org/spdy/spdy-whitepaper).
- Regarding the OkHttp version, this project is based on [OkHttp 2.0.0 release](https://github.com/square/okhttp/releases/tag/parent-2.0.0) code.
- Regarding why "okhttp-all-in-one", `package` all code in one `jar`, it's just convenient to client using.
- Regarding the license, this project follows the same license with OkHttp, [Apache License, Version 2.0 ](https://github.com/hostinmars/spdymcsclient/blob/master/LICENSE).
- Regarding the latest study of SPDY and mobile web service, it is a reference ["Towards a SPDY'ier mobile web?"](http://dl.acm.org/citation.cfm?id=2535372.2535399&coll=DL&dl=GUIDE&CFID=512794707&CFTOKEN=71508233). So you should know the limitation of mobile service SPDY refining definitely.
- And more study, [here](http://dl.acm.org/results.cfm?h=1&cfid=512794707&cftoken=71508233).
<p>


We contribute [this feature](https://github.com/square/okhttp/pull/985) to the OkHttp project, too.
<br>**NOTE:** According the review of [swankjesse](https://github.com/swankjesse) -the author of OkHttp, it doesn't make sense to include this feature to OkHttp,
since the [HTTP 2](http://en.wikipedia.org/wiki/HTTP_2.0) has cancelled the plan of using TLS as one mandatory part.
<br>We use this project for Android and desktop SPDY client engine, for iOS platform, we use [CocoaSPDY](https://github.com/twitter/CocoaSPDY) as SPDY client engine.

### Meaning of this project
The goals of this project are:
- Study the [OkHttp](https://github.com/square/okhttp), lean how to use the OkHttp lib for standard SPDY connection layered on modern TLS.
- Try to modify OkHttp lib to support preferred protocol for specify host especially for SPDY connection with the **http** host and 80 port.
- Try to test and verify the viability of above modification.
- Regarding viability, we proved it with benchmark code: `Benchmark1.java` and `Benchmark2.java`.

Actually, The [SPDY](http://dev.chromium.org/spdy/spdy-whitepaper) is layered on top of the SSL, so the default implementation of OkHttp lib strictly follows the definition of SPDY and http2.
<br>Consider of the easier or more convenient way for some **old http** service refining with SPDY, and making the different mobile platforms(especially the Android and iOS) can access the same web service, so we invoked efforts on this project.
<p>
### The nginx proxy configuration for SPDY host
It's a reference:
```
 server {

        listen       80 spdy;
        server_name  api.m.renren.com;
        add_header Alternate-Protocol  443:npn-spdy/3;
        spdy_headers_comp 1;

        location / {
            proxy_pass    http://10.3.20.108:80;#MCS real server
            proxy_redirect     off;
            proxy_set_header   Host             $host;
            proxy_set_header   X-Real-IP        $remote_addr;
            proxy_set_header   X-Forwarded-For  $proxy_add_x_forwarded_for;
            client_max_body_size       50m;
        }
}
```
<p>

### NOTE
>- When you set preferred SPDY protocol for **http** request, which means you also ignored the [npn or alpn](http://en.wikipedia.org/wiki/Next_Protocol_Negotiation) protocol negotiate feature.
>- But for **https** request, the [npn or alpn](http://en.wikipedia.org/wiki/Next_Protocol_Negotiation) protocol negotiate feature has higher priority than preferred protocol.
>- For more details, you can check the spdy-client code, especially the comments of App.java.

### Protocol Decision
In real project, you will need make choice for protocol using, and meet the question:
>- What kind of protocol should be chosen and how it's suitable?
Bellow figure shows us some suggestions, which comes from this study page, [here](https://www.usenix.org/conference/nsdi14/technical-sessions/wang).
<br>![choose protocol](https://raw.githubusercontent.com/hostinmars/spdymcsclient/master/ext/tree.jpg)

### Compile and run
- maven setting: It's a [reference](http://maven.oschina.net/help.html).
- `cd okhttp-all-in-one` *and* `mvn clean package -U` *and* `mvn install`
- `cd spdy-client` *and*
- `mvn clean package -U -DskipTests`
- `mvn exec:exec`

