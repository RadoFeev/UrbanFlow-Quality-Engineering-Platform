package io.urbanflow.testcore.junit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Serves production portal assets from isolated loopback origins for non-functional browser tests.
 */
public final class PortalServerExtension implements BeforeAllCallback {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(PortalServerExtension.class);

  @Override
  public void beforeAll(ExtensionContext context) {
    context
        .getRoot()
        .getStore(NAMESPACE)
        .getOrComputeIfAbsent("portal-servers", ignored -> ServerGroup.start(), ServerGroup.class);
  }

  private static final class ServerGroup
      implements ExtensionContext.Store.CloseableResource, AutoCloseable {

    private final ExecutorService executor;
    private final List<HttpServer> servers;

    private ServerGroup(ExecutorService executor, List<HttpServer> servers) {
      this.executor = executor;
      this.servers = servers;
    }

    static ServerGroup start() {
      Path workspace = Path.of(System.getProperty("workspace.root")).toAbsolutePath().normalize();
      ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
      List<HttpServer> servers = new ArrayList<>();
      Map<String, String> portals =
          Map.of(
              "customer.url", "apps/customer-portal/src/main/resources/static",
              "courier.url", "apps/courier-portal/src/main/resources/static",
              "operations.url", "apps/operations-dashboard/src/main/resources/static");
      try {
        portals.forEach(
            (property, relativeRoot) -> {
              HttpServer server = startServer(workspace.resolve(relativeRoot), executor);
              servers.add(server);
              System.setProperty(
                  property, "http://127.0.0.1:" + server.getAddress().getPort() + "/");
            });
        return new ServerGroup(executor, List.copyOf(servers));
      } catch (RuntimeException exception) {
        servers.forEach(server -> server.stop(0));
        executor.close();
        throw exception;
      }
    }

    private static HttpServer startServer(Path root, ExecutorService executor) {
      if (!Files.isDirectory(root)) {
        throw new IllegalStateException("portal asset directory does not exist: " + root);
      }
      try {
        HttpServer server =
            HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> serve(exchange, root));
        server.setExecutor(executor);
        server.start();
        return server;
      } catch (IOException exception) {
        throw new IllegalStateException("could not start portal fixture server", exception);
      }
    }

    private static void serve(HttpExchange exchange, Path root) throws IOException {
      try (exchange) {
        if (!"GET".equals(exchange.getRequestMethod())) {
          exchange.sendResponseHeaders(405, -1);
          return;
        }
        String requested =
            "/".equals(exchange.getRequestURI().getPath())
                ? "index.html"
                : exchange.getRequestURI().getPath();
        Path file = root.resolve(requested.replaceFirst("^/", "")).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
          exchange.sendResponseHeaders(404, -1);
          return;
        }
        exchange.getResponseHeaders().set("Content-Type", contentType(file));
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        byte[] body = Files.readAllBytes(file);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
      }
    }

    private static String contentType(Path file) {
      String name = file.getFileName().toString();
      if (name.endsWith(".html")) {
        return "text/html; charset=utf-8";
      }
      if (name.endsWith(".css")) {
        return "text/css; charset=utf-8";
      }
      if (name.endsWith(".js")) {
        return "text/javascript; charset=utf-8";
      }
      return "application/octet-stream";
    }

    @Override
    public void close() {
      servers.forEach(server -> server.stop(0));
      executor.close();
    }
  }
}
