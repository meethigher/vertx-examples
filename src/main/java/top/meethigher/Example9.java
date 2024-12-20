package top.meethigher;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class Example9 {


    private static final Vertx vertx = Vertx.vertx();

    public static void main(String[] args) {
        MemoryMonitor memoryMonitor = new MemoryMonitor();
        memoryMonitor.start();
        log.info("start {}", memoryMonitor.convertBytes(memoryMonitor.getUsedMemory()));
        vertx.setPeriodic(1000, v -> {
            log.info("cur {}", memoryMonitor.convertBytes(memoryMonitor.getUsedMemory()));
        });
        httpServer();
    }

    /**
     * 使用postman或者httpie自行进行测试
     */
    private static void httpServer() {
        HttpServer httpServer = vertx.createHttpServer();
        FileSystem fs = vertx.fileSystem();
        Router router = Router.router(vertx);
        BodyHandler bodyHandler = BodyHandler.create();//处理请求体, 拿掉就解析不到请求体咯


        /**
         * 其他类型请求体，比如json、xml等等
         *
         * curl -X POST -H "Content-Type:application/json" -d "{ \"companyName\": \"\", \"pageIndex\": 1, \"pageSize\": 20}" "http://10.0.0.1:4321/test/raw"
         * curl -X POST -H "Content-Type:application/xml" -d "<dependency><groupId>io.vertx</groupId><artifactId>vertx-core</artifactId><version>${vertx.version}</version></dependency>" "http://10.0.0.1:4321/test/raw"
         */
        router.route(HttpMethod.POST, "/test/raw")
                .consumes("application/json")
                .consumes("application/xml")
                .handler(bodyHandler)
                .handler(rc -> {
                    String string = rc.body().asString();
                    System.out.println(string);
                    System.out.println(string.length());
                    //设置最大长度，防止有人恶意传输超长的json，占用cpu资源去解析
                    JsonObject jsonObject = rc.body().asJsonObject(2);
//                    JsonObject jsonObject = rc.body().asJsonObject();
//                    Map pojo = rc.body().asPojo(Map.class);// 以json形式进行转换
                    rc.response().end();
                });

        /**
         * multipart/form-data
         *
         * curl -X POST -F file=@server.properties -F sleep=0 "http://10.0.0.1:4321/test/upload?t=1"
         */
        router.route(HttpMethod.POST, "/test/upload")
                .consumes("multipart/form-data")
                // 整体流程跟tomcat类似：用户上传文件后，程序先将文件写入到某个地方，然后再执行实际的业务代码，最后删除。
                .handler(BodyHandler.create().setBodyLimit(50 * 1024 * 1024 * 1024L).setDeleteUploadedFilesOnEnd(true)).handler(rc -> {
                    MultiMap entries = rc.request().formAttributes();
                    for (Map.Entry<String, String> entry : entries) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                    String path = "";
                    for (FileUpload fileUpload : rc.fileUploads()) {
                        String name = fileUpload.name();
                        System.out.println(name);
                        path = fileUpload.uploadedFileName();
                        System.out.println(path);
                        String s1 = fileUpload.contentTransferEncoding();
                        System.out.println(s1);
                    }
                    // 读取文件
                    fs.copy(path, "D:/Desktop/abcd").onSuccess(t -> {
                        rc.response().end();
                    });
                });
        httpServer.requestHandler(router);
        httpServer.listen(4321).onSuccess(s -> {
            System.out.println("HTTP server started on port 4321");
        }).onFailure(Throwable::printStackTrace);
    }


}
