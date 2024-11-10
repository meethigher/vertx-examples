package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.proxy.handler.ProxyHandler;
import io.vertx.httpproxy.HttpProxy;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Example6 {

    public static final Vertx vertx = Vertx.vertx();

    public static void defaultHandler(Router router, HttpProxy httpProxy, HttpProxy httpsProxy) {
        // 默认处理逻辑ProxyHandler，这个是由vertx-web-proxy扩展提供
        router.route("/halo/*").handler(ProxyHandler.create(httpProxy, 4321, "10.0.0.1"));
        router.route().handler(ProxyHandler.create(httpsProxy, 443, "meethigher.top"));

        // 第一种自定义方式，使用注入器
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(false).setTrustAll(true).setVerifyHost(false));
        HttpProxy host = HttpProxy.reverseProxy(httpClient).origin(4321, "10.0.0.1");
        //host.addInterceptor(new ProxyInterceptor() {
        //    @Override
        //    public Future<ProxyResponse> handleProxyRequest(ProxyContext context) {
        //        context.request().headers().set("host", "meethigher.top");// 这个请求头修改不了，需要深入到HttpClient内部才能进行修改。
        //        context.request().headers().set("Axb", "Axb"); // 这个请求头就可以改
        //        return ProxyInterceptor.super.handleProxyRequest(context);
        //    }
//
        //    @Override
        //    public Future<Void> handleProxyResponse(ProxyContext context) {
        //        context.response().headers().set("CCC", "CCC");
        //        return ProxyInterceptor.super.handleProxyResponse(context);
        //    }
        //});
        //ProxyHandler requestHandler = ProxyHandler.create(host);
        //router.route("/test/*").order(Integer.MIN_VALUE).handler(requestHandler);

        // 第二种注入方式。在handler中自己决定何时使用HttpProxy
        router.route("/test/*").order(Integer.MIN_VALUE).handler(context -> {
            context.request().headers().set("host", "meethigher.top");// 这个请求头修改不了，需要深入到HttpClient内部才能进行修改。
            context.request().headers().set("Axb", "Axb"); // 这个请求头就可以改
            host.handle(context.request());
            context.response().putHeader("CCC", "CCC");
        });
    }

    public static void diyHandler(Router router, HttpClient httpClient, HttpClient httpsClient) {
        // 自定义逻辑，自己实现。order越小，优先级越高
        // /api/* --> https://reqres.in/api/*
        router.route("/api/*").order(Integer.MIN_VALUE).handler(ctx -> {
            HttpServerRequest request = ctx.request();
            String uri = request.uri();
            Route route = ctx.currentRoute();
            String path;
            /**
             * false表示 /api/* --> https://reqres.in/api/*
             * true表示 /api/* --> https://requres.in/*
             */
            boolean whole = false;
            if (whole) {
                if (route.getPath().endsWith("/")) {
                    int length = route.getPath().length() - 1;
                    path = uri.substring(length);
                } else {
                    int length = route.getPath().length();
                    path = uri.substring(length);
                }
            } else {
                path = request.uri();
            }
            System.out.println(path);
            httpClient.request(new RequestOptions().setMethod(request.method()).setURI(path).setHost("reqres.in").setPort(443).setSsl(true))
//            httpsClient.request(HttpMethod.valueOf(request.method().name()), 443, "reqres.in", path)
                    .onSuccess(r -> {
                        r.headers().setAll(request.headers());
                        r.putHeader("Host", "reqres.in");
                        r.send()
                                .onSuccess(r1 -> {
                                    ctx.response()
                                            .setStatusCode(r1.statusCode())
                                            .headers().setAll(r1.headers());
                                    r1.handler(data -> {
                                        ctx.response().write(data);
                                    });
                                    r1.endHandler(v -> ctx.response().end());

                                })
                                .onFailure(e1 -> {
                                    ctx.response().setStatusCode(500).end(e1.getMessage());
                                });
                    })
                    .onFailure(e -> {
                        ctx.response().setStatusCode(500).end("Internal Server Error");
                    });
        });
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setTrustAll(true).setVerifyHost(false).setLogActivity(true));
        HttpClient httpsClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

        HttpProxy httpProxy = HttpProxy.reverseProxy(httpClient);
        HttpProxy httpsProxy = HttpProxy.reverseProxy(httpsClient);

        defaultHandler(router, httpProxy, httpsProxy);
        diyHandler(router, httpClient, httpsClient);


        httpServer.requestHandler(router).listen(port).onSuccess(t -> {
            log.info("http server started on port {}", port);
        });

        TimeUnit.MINUTES.sleep(10);
        // 可以在运行时，动态移除路由
        List<Route> routes = router.getRoutes();
        for (Route next : routes) {
            // 首先判断是否是精准匹配地址
            String path = next.isExactPath() ? next.getPath() : next.getPath() + "*";
            if ("/api/*".equals(path)) {
                next.remove();
            }
        }
        log.info("remove route /api/*");
        List<Route> routes1 = router.getRoutes();
        System.out.println();
    }
}
