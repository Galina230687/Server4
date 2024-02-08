import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Optional;


public class Server {

    public static final String GET = "GET";
    public static final String POST = "POST";
    private final int port;
    final static int THREAD_COUNT = 64;
    private final ExecutorService threadPool;
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");


    public Server(int port) {
        this.port = port;
        threadPool = Executors.newFixedThreadPool(THREAD_COUNT);

    }


    public void start() {
        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.execute(() -> doConnect(socket));
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }


    public void doConnect(Socket socket) {

        try (
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {

            final var limit = 4096;

            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);


            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
                System.out.println("Bad request exception 1");
                return;
            }

            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                badRequest(out);
                System.out.println("Bad request exception 2");
                return;
            }

            final var method = requestLine[0];
            final var allowedMethods = List.of(GET, POST);
            if (!allowedMethods.contains(method)) {
                badRequest(out);
                System.out.println("Bad request exception 3");
                return;
            }
            System.out.println(method);

            final var path = requestLine[1];
            if (!path.startsWith("/")) {
                badRequest(out);
                System.out.println("Bad request exception 4");
                return;
            }
            System.out.println(path);


            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                badRequest(out);
                return;
            }

            in.reset();
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
            System.out.println(headers);


            if (!method.equals(GET)) {
                in.skip(headersDelimiter.length);

                final var contentLength = extractHeader(headers, "Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());
                    final var bodyBytes = in.readNBytes(length);

                    final var body = new String(bodyBytes);
                    System.out.println(body);
                }
            }

            final var path2 = requestLine[1];
            if (!validPaths.contains(path2)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);


            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
                return;
            }


            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        System.out.println("Bad request exception executes!");
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }


    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }


}
