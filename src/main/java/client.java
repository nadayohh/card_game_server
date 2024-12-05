import org.json.JSONObject;

import java.io.*;
import java.net.Socket;

public class client extends Thread{
    private Socket sock;
    private BufferedReader br;
    private PrintWriter pw;
    private boolean login;
    private User user;
    private game game;
    private room room;
    private boolean inGame;
    private userManager manager;
    private volatile boolean terminationFlag = false;
    private messageGenerator mg;

    public client(userManager manager, Socket sock){
        this.sock = sock;
        this.manager = manager;
        mg = new messageGenerator();
        gameList.getInstance().init(this.mg);
        try{
            pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            login = false;
        }catch(Exception ex){
            System.out.println(ex);
        }
    }

    public void run(){
        try{
            String data;
            while((data=br.readLine())!=null && !terminationFlag){
                JSONObject requestRoot = new JSONObject(data);
                System.out.println(requestRoot);
                String request = requestRoot.getString("request");
                JSONObject requestData = requestRoot.getJSONObject("data");
                if(request.equals("login")){
                    String id = requestData.getString("id");
                    String pwd = requestData.getString("pwd");
                    this.user = manager.UserExist(id, pwd);
                    if(this.user==null){
                        sendMessage(mg.errorMessage("loginfail").toString());
                    }else{
                        login = true;
                        sendMessage(mg.loginSuccess(gameList.getInstance()).toString());
                    }
                }else if(request.equals("register")){
                    String id = requestData.getString("id");
                    String pwd = requestData.getString("pwd");
                    if(manager.idExist(id)){
                        sendMessage(mg.errorMessage("registerfail").toString());
                    }else{
                        if(manager.createUser(id, pwd)){
                            sendMessage(mg.registerSuccess().toString());
                        }else{
                            sendMessage(mg.errorMessage("registerfail").toString());
                        }
                    }
                }else if(request.equals("selectGame")){
                    int gameId = Integer.parseInt(requestData.getString("gameNum"));
                    this.game = gameList.getInstance().getGameInstance(gameId);
                    sendMessage(mg.sendRoomList(game).toString());
                }else if(request.equals("selectRoom")){
                    int roomId = Integer.parseInt(requestData.getString("roomId"));
                    this.room = game.enterRoom(roomId, this);
                }else if(request.equals("start")){
                    if(room.isLeader(this)){
                        room.start();
                    }else{
                        sendMessage(mg.errorMessage("you are not leader").toString());
                    }
                }else{
                    room.handleClientMessage(data, this);
                }
            }
        }catch(Exception ex){System.out.println(ex);}
    }

    public void sendMessage(String message){
        pw.println(message);
        pw.flush();
    }

    public User getUserInstance(){
        return this.user;
    }

    public void changeStatus(boolean inGame){this.inGame = inGame;}

    public void stopThread(){
        terminationFlag = true;
    }
}
