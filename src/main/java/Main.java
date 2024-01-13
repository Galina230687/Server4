import java.net.http.HttpClient;

public class Main {
    public static void main(String[] args){

        HttpClient client = (HttpClient) HttpClient.newBuilder();

        Server server = new Server(9999);
        server.start();
    }
}
