public class Client {

    public static void main(String[] args) {

        Server server = new Server(9999);
        server.start();

    }
}
