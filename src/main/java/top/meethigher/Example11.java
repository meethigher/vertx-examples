package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Example11 {

    private static Vertx vertx = Vertx.vertx();

    public static void main(String[] args) {
        demo1();
    }

    public static void demo1() {
        WebClient client = WebClient.create(vertx);

        client.get(443, "reqres.in", "/api/users?page=1")
                .ssl(true)
                .send()
                .onSuccess(response -> System.out.println("Received response with status code " + response.statusCode()))
                .onFailure(err -> System.out.println("Something went wrong " + err.getMessage()));

    }
}
