package top.meethigher;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Example16 {


    private static final Logger log = LoggerFactory.getLogger(Example16.class);
    public static final String NEW_SESSION = "NEW_SESSION:";
    public static final String SESSION_ID = "SESSION_ID:";
    private static final Vertx vertx = Vertx.vertx(new VertxOptions().setMaxEventLoopExecuteTime(Duration.ofDays(1).toNanos()));

    public static void main(String[] args) {
        String mode = System.getProperty("mode");
        if ("server".equals(mode)) {
            vertx.deployVerticle(new Frps());
        } else {
            vertx.deployVerticle(new Frpc());
        }
    }


    private static class Frps extends AbstractVerticle {

        private Set<NetSocket> controlSockets = ConcurrentHashMap.newKeySet();
        private Map<String, Connection> waitBindSessionMap = new ConcurrentHashMap<>();

        private void handleAsyncResult(AsyncResult<NetServer> ar) {
            if (ar.succeeded()) {
                log.info("server started on port {}", ar.result().actualPort());
            } else {
                log.error("server start failed", ar.cause());
                System.exit(1);
            }
        }

        /**
         * 控制连接处理逻辑
         */
        private void handleControlConnection(NetSocket socket) {
            socket.pause();
            log.info("control connection established: {}", socket.remoteAddress());
            controlSockets.add(socket);
            socket.closeHandler(v -> {
                controlSockets.remove(socket);
                log.info("control connection closed: {}", socket.remoteAddress());
            });
            socket.resume();
        }

        /**
         * 代理连接(用户连接/数据连接)处理逻辑
         */
        private void handleProxyConnection(NetSocket socket) {
            socket.pause();

            // demo只做整体流程展示，故不考虑tcp数据传输中的问题
            socket.handler(buffer -> {
                String message = buffer.toString();
                // 判断数据连接和用户连接
                if (message.startsWith(SESSION_ID)) {
                    // 数据连接
                    String sessionId = message.substring(SESSION_ID.length());
                    Connection userConn = waitBindSessionMap.remove(sessionId);
                    if (userConn != null) {
                        bindConnections(userConn, socket, sessionId);
                    } else {
                        log.info("invalid session id {}", sessionId);
                        socket.close();
                    }
                } else {
                    // 用户连接
                    String sessionId = UUID.randomUUID().toString();
                    Connection userConn = new Connection(sessionId, socket, new ArrayList<>());
                    userConn.buffers.add(buffer.copy());
                    waitBindSessionMap.put(sessionId, userConn);
                    log.info("user connection established: {}, sessionId: {}", socket.remoteAddress(), sessionId);
                    // 用户连接进来，需要通过控制连接通知frpc主动建立数据连接
                    for (NetSocket controlSocket : controlSockets) {
                        controlSocket.write(NEW_SESSION + sessionId);
                    }
                    // 数据连接尚未接入进来时，将用户连接的数据进行缓存
                    socket.handler(b -> userConn.buffers.add(b.copy()));
                    // 指定时限内，未有新增过来的数据连接与用户连接进行绑定，则连接释放。
                    setupSessionTimeout(sessionId, socket);
                }
            });
            socket.resume();
        }

        private void bindConnections(Connection userConn, NetSocket dataSocket, String sessionId) {
            NetSocket userSocket = userConn.socket;

            // 双向连接进行生命周期的绑定，并进行双向数据转发
            userSocket.closeHandler(v -> {
                log.info("userSocket {} closed", userSocket.remoteAddress());
                dataSocket.close();
            }).pipeTo(dataSocket);
            dataSocket.closeHandler(v -> {
                log.info("dataSocket {} closed", dataSocket.remoteAddress());
                userSocket.close();
            }).pipeTo(userSocket);

            // 将用户连接中缓存的数据发出
            userConn.buffers.forEach(dataSocket::write);

            log.info("data connection {} bound for session: {}", dataSocket.remoteAddress(), sessionId);
        }

        private void setupSessionTimeout(String sessionId, NetSocket socket) {
            vertx.setTimer(30000, tid -> {
                if (waitBindSessionMap.remove(sessionId) != null) {
                    log.info("session timeout: " + sessionId);
                    socket.close();
                }
            });
        }

        @Override
        public void start() throws Exception {
            int controlPort = 44444;
            int dataProxyPort = 2222;

            vertx.createNetServer(new NetServerOptions().setTcpNoDelay(true))
                    .connectHandler(this::handleControlConnection)
                    .listen(controlPort)
                    .onComplete(this::handleAsyncResult);
            vertx.createNetServer(new NetServerOptions().setTcpNoDelay(true))
                    .connectHandler(this::handleProxyConnection)
                    .listen(dataProxyPort)
                    .onComplete(this::handleAsyncResult);
        }
    }

    private static class Frpc extends AbstractVerticle {

        private NetSocket controlSocket;

        private NetClient netClient;

        private Map<NetSocket, NetSocket> waitBindConnections = new ConcurrentHashMap<>();

        @Override
        public void start() throws Exception {
            netClient = vertx.createNetClient(new NetClientOptions().setTcpNoDelay(true));

            netClient.connect(44444, "127.0.0.1").onComplete(this::handleControlSocket);
        }

        private void handleControlSocket(AsyncResult<NetSocket> ar) {
            if (ar.succeeded()) {
                controlSocket = ar.result();
                controlSocket.pause();
                log.info("connected to control server");
                controlSocket.handler(this::handleControlMessage);
                controlSocket.closeHandler(v -> System.out.println("Control connection closed"));
                controlSocket.resume();
            } else {
                log.error("connect error", ar.cause());
                System.exit(1);
            }
        }

        private void handleDataSocket(AsyncResult<NetSocket> ar, String sessionId) {
            if (ar.succeeded()) {
                NetSocket socket = ar.result();
                socket.pause();
                // 与backend建立真实连接
                netClient.connect(2222, "meethigher.top").onComplete(ar1 -> {
                    if (ar1.succeeded()) {
                        NetSocket realSocket = ar1.result();
                        realSocket.pause();
                        realSocket.closeHandler(v -> socket.close()).handler(b -> {
                            System.out.println("realSocket-->socket: " + b);
                            socket.write(b);
                        });
                        socket.closeHandler(v -> realSocket.close()).handler(b -> {
                            System.out.println("socket-->realSocket: " + b);
                            realSocket.write(b);
                        });
                        realSocket.resume();
                        socket.resume();
                    } else {
                        log.info("real connection established error", ar1.cause());
                        socket.close();
                    }
                });


                socket.write(Buffer.buffer(SESSION_ID + sessionId));
                log.info("data connection established for session: {}", sessionId);

            } else {
                log.error("failed to connect data proxy server", ar.cause());
            }
        }

        private void handleControlMessage(Buffer buffer) {
            String message = buffer.toString();
            if (message.startsWith(NEW_SESSION)) {
                String sessionId = message.substring(NEW_SESSION.length());
                log.info("received data request for session: {}", sessionId);

                // 与frps建立数据连接
                netClient.connect(2222, "127.0.0.1").onComplete(ar -> handleDataSocket(ar, sessionId));
            }
        }
    }

    private static class Connection {
        public final String sessionId;
        public final NetSocket socket;
        public final List<Buffer> buffers;


        private Connection(String sessionId, NetSocket socket, List<Buffer> buffers) {
            this.sessionId = sessionId;
            this.socket = socket;
            this.buffers = buffers;
        }
    }
}
