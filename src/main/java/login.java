import java.io.*;
import java.net.*;
import org.json.JSONObject;

public class login extends Thread{
    private Socket sock;
    private String id;
    private BufferedReader br;
    private PrintWriter pw;
    private boolean login = false;

    public login(Socket sock){
        this.sock = sock;
        try{
            pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            login = false;
        }catch(Exception ex){System.out.println(ex);}
    }

    public void run(){
        try{
            String data;
            while((data=br.readLine())!=null){
                JSONObject jsonRoot = new JSONObject(data);
                String header = jsonRoot.getString("header");
                JSONObject jsonData = jsonRoot.getJSONObject("body");

            }
        }catch(Exception ex){System.out.println(ex);}
    }
}
