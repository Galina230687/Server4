import java.io.*;
import java.net.Socket;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/*
   Статический класс-обработчик поступающих на сервер запросов.
 */
public class RequestHandler {

    private static final String FREE_SPACE = "<br><br><br>";
    private static final String LOG_DELIMITER = " ---------------------   ---------------------   ----------- \n";

    private static final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    public static void handleRequest(Socket socket) throws IOException {


        try (final BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
             final BufferedOutputStream os = new BufferedOutputStream(socket.getOutputStream())) {

            Request request = new Request(in);
            //Не можем распарсить запрос:
            if (!request.parse()) {
                respond(500, "Unable to parse request", os);
                return;
            }

            String path = request.getPath();
            //Запрашиваемого ресурса не существует на сервере:
            if (!validPaths.contains(request.getPath())) {
                respond(404, "Not Found", os);
            }

            System.out.println(LOG_DELIMITER);
            System.out.println("Request method: " + request.getMethod());
            System.out.println("Request path: " + request.getPath());
            System.out.println("Request Query: " + request.getQueryParameters());
            System.out.println(LOG_DELIMITER);

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);
            String responseHtml = null;

            if (request.getPath().equals("/classic.html")) {
                String additionalData = "";
                if (request.getQueryParameters().isEmpty()) {
                    additionalData = LocalDateTime.now().toString();
                } else {
                    additionalData = handleRequestMethod(request);
                    System.out.println("additionalData is: " + additionalData);
                }
                final String template = Files.readString(filePath);
                responseHtml = template.replace("<p>Current time is: {time}</p>", additionalData);
                os.write(getResponse((long) responseHtml.length(), "text/html").getBytes());
                os.write(responseHtml.getBytes());
            } else {
                if (Files.exists(filePath)) {
                    final var length = Files.size(filePath);
                    os.write(getResponse(length, mimeType).getBytes());
                    Files.copy(filePath, os);
                }
            }

            os.flush();
            in.close();
        }
    }

    private static String getResponse(Long contentLength, String contentType) {
        return "HTTP/1.1 200 OK\r\n"
                + "Server: Server\r\n"
                + "Content-Type: "
                + contentType
                + "\r\n"
                + "Content-Length: "
                + contentLength
                + "\r\n"
                + "Connection: close\r\n\r\n";
    }

    private static void respond(int statusCode, String msg, OutputStream out) throws IOException {
        String responseLine = "HTTP/1.1 " + statusCode + " " + msg + "\r\n\r\n";
        out.write(responseLine.getBytes());
    }

    private static String handleRequestMethod(Request request) {
        StringBuffer generatedRequestHtml = new StringBuffer();
        generatedRequestHtml.append(FREE_SPACE);
        if (!request.getQueryParameters().isEmpty()) {
            generatedRequestHtml.append("<p>ПАРАМЕТРЫ ЗАПРОСА:</p>");
            request.getQueryParameters().forEach(
                    (key, value) ->
                            generatedRequestHtml.append("<p style='margin: 0; padding: 0;'><b>").append(key).append(": </b>").append(value).append("</p>"));
        }
        generatedRequestHtml.append(FREE_SPACE);
        if (!request.getHeaders().isEmpty()) {
            generatedRequestHtml.append("<p>ЗАГОЛОВКИ:</p>");
            request.getHeaders().forEach(
                    (key, value) ->
                            generatedRequestHtml.append("<p style='margin: 0; padding: 0;'><b>").append(key).append(": </b>").append(value).append("</p>"));
        }


        return String.valueOf(generatedRequestHtml);
    }
}
