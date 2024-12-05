import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static final int PORT_NUMBER = 10000;
    public static void main(String[] args) {
        userManager manager = new userManager();
        try(ServerSocket server = new ServerSocket(PORT_NUMBER)){
            while(true){
                Socket connection = server.accept();
                client cl = new client(manager, connection);
                cl.start();
            }
        }catch(IOException e){
            System.out.println(e);
        }
    }
}