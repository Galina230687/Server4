import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;


public class Server {
    private final int port;
    final static int THREAD_COUNT = 64;
    private final ExecutorService threadPool;

    public Server(int port) {
        this.port = port;
        threadPool = Executors.newFixedThreadPool(THREAD_COUNT);

    }


    public void start() {
        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.execute(() -> {
                    try {
                        RequestHandler.handleRequest(socket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
