import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class Request {

    private String method;
    private String path;
    private String fullMethodRequest;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> queryParameters = new HashMap<String, String>();
    private BufferedInputStream bufferedInputStream;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFullMethodRequest() {
        return fullMethodRequest;
    }

    public void setFullMethodRequest(String fullMethodRequest) {
        this.fullMethodRequest = fullMethodRequest;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(Map<String, String> queryParameters) {
        this.queryParameters = queryParameters;
    }

    public BufferedInputStream getInputReader() {
        return bufferedInputStream;
    }

    public void setInputReader(BufferedInputStream bufferedInputStream) {
        this.bufferedInputStream = bufferedInputStream;
    }

    public Request(BufferedInputStream bufferedInputStream) {

        this.bufferedInputStream = bufferedInputStream;
    }

    private void parseQueryParameters(String queryString) {
        List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8);


       /* for (int i = 0; i < nameValuePairs.size(); i++) {
            System.out.println("Parameter key is: " + nameValuePairs.get(0).getName() + " parameter value is: " + nameValuePairs.get(0).getValue());
        }*/

    }

    public boolean parse() throws IOException {
        final int limit = 4096;

        bufferedInputStream.mark(limit);
        final byte[] buffer = new byte[limit];
        final int read = bufferedInputStream.read(buffer);


        final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
        final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            System.out.println("Bad request exception 1");
            return false;
        }

        String[] requestComponents = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");

        if (requestComponents.length != 3) {
            System.out.println("Bad request exception 2");
            return false;
        }

        this.method = requestComponents[0];
        final var allowedMethods = List.of("GET", "POST");
        if (!allowedMethods.contains(method)) {
            System.out.println("Bad request exception 3");
            return false;
        }

        this.path = requestComponents[1];
        if (!path.startsWith("/")) {
            System.out.println("Bad request exception 4");
            return false;
        }
        //Проверяем, есть ли в запросе параметры:
        if (requestComponents[1].contains("?")) {
            path = requestComponents[1].substring(0, requestComponents[1].indexOf("?"));

            //Парсим параметры запроса:
            String parameters = requestComponents[1].substring(requestComponents[1].indexOf("?") + 1);
            List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(parameters, StandardCharsets.UTF_8);
            for (NameValuePair nameValuePair : nameValuePairs) {
                queryParameters.put(nameValuePair.getName(), nameValuePair.getValue());
                System.out.println("Parameter key is: " + nameValuePair.getName() + " parameter value is: " + nameValuePair.getValue());
            }
        }

        //Если Post-запрос, то ответ приходит без пути, поэтому, подменяем путь, на путь к файлу index.html:
        if (path.equals("/")) {
            path = "/classic.html";
        }


        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            return false;
        }

        bufferedInputStream.reset();
        bufferedInputStream.skip(headersStart);

        final var headersBytes = bufferedInputStream.readNBytes(headersEnd - headersStart);
        String headersLine = new String(headersBytes);
        // System.out.println("headers line is: " + headersLine);
        int separator = headersLine.indexOf(":");
        if (separator != -1) {

            headers.put(headersLine.substring(0, separator), headersLine.substring(separator + 1));
        }


        final var headersList = Arrays.asList(new String(headersBytes).split("\r\n"));

        if (!method.equals("GET")) {
            bufferedInputStream.skip(headersDelimiter.length);

            final var contentLength = extractHeader(headersList, "Content-Length");
            if (contentLength.isPresent()) {
                int length = Integer.parseInt(contentLength.get());
                byte[] bodyBytes = bufferedInputStream.readNBytes(length);

                final var body = new String(bodyBytes);
                parseQueryParameters(body);
                System.out.println("request body is: " + body);
            }
        }

        return true;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
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