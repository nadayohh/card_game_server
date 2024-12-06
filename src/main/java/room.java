import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class room extends Thread{
    private String roomName;
    private volatile List<client> players = new CopyOnWriteArrayList<>();
    private volatile Map<String, Boolean> activePlayers = new ConcurrentHashMap<>();
    private volatile client leader;
    private volatile dealerI dealer;
    private int maxPlayer = 8;
    private AtomicBoolean gameInProgress = new AtomicBoolean(false);
    private messageGenerator mg;
    private int gameId;

    public room(String roomName, messageGenerator mg, int gameId){
        this.mg = mg;
        this.roomName = roomName;
        this.gameId = gameId;
    }

    public String getRoomName(){
        return this.roomName;
    }

    public void join(client client){
        if(numberOfPlayer() == 0) {
            leader = client;
        }
        broadcast(mg.updateRoomState(this).toString());
        if(numberOfPlayer() > maxPlayer){
            client.sendMessage(mg.errorMessage("enter Failed").toString());
        }
        if(gameInProgress.get()){
            players.add(client);
            activePlayers.put(client.getName(), false);
        }else{
            players.add(client);
            activePlayers.put(client.getName(), true);
        }
        client.sendMessage(mg.updatePlayerState(activePlayers.get(client.getName())).toString());
    }

    public void leave(client player){
        players.remove(player);
        activePlayers.remove(player.getName());
    }

    public void broadcast(String message){
        for(client client : players){
            client.sendMessage(message);
        }
    }

    public int numberOfPlayer(){
        return players.size();
    }

    public void run(){
        while(true){
            if(gameId==0){
                dealer = new blackJackDealer(this, mg);
            }else if(gameId==1){
                dealer = new casinoWarDealer(this, mg);
            }else{
                dealer = new baccaratDealer(this, mg);
            }
            gameInProgress.set(true);
            dealer.play(players, activePlayers, numberOfActivePlayer());
            gameInProgress.set(false);
            while(!gameInProgress.get());
        }
    }

    public boolean isLeader(client player){
        return leader == player;
    }

    public void changeRoomState(boolean flag){
        gameInProgress.set(flag);
    }

    private int numberOfActivePlayer(){
        int tmp = 0;
        for(client player : players){
            if(activePlayers.get(player.getName()))tmp++;
        }
        return tmp;
    }

    public void broadcastTimer(int remainingTime){
        JSONObject message = new JSONObject();
        message.put("response", "timer_update");
        message.put("data", new JSONObject().put("remaining_time", remainingTime));
        broadcast(message.toString());
    }

    public void broadcastGameUpdate(String playerTurn, int currentBet){
        broadcast(mg.gameUpdate(playerTurn, currentBet).toString());
    }

    public void handleClientMessage(String clientMessage, client player){
        try{
            JSONObject requestRoot = new JSONObject(clientMessage);
            String request = requestRoot.getString("request");
            JSONObject data = requestRoot.getJSONObject("data");
            switch(request){
                case "bet":
                    if(!Objects.equals(dealer.checkPlayerTurn().getName(), player.getName())){
                        player.sendMessage(mg.errorMessage("not your turn").toString());
                        return;
                    }
                    int amount = data.optInt("amount", 0);
                    String bet = data.getString("bet");
                    dealer.handleBet(amount, bet);
                    break;
                case "hitOrStand":
                    if(!Objects.equals(dealer.checkPlayerTurn().getName(), player.getName())){
                        player.sendMessage(mg.errorMessage("not your turn").toString());
                        return;
                    }
                    String hitOrStand = data.getString("hitOrStand");
                    dealer.playRounds(hitOrStand);
                    break;
                case "call":
                    int additionalBet = data.optInt("amount", 0);
                    String gotoWar = data.getString("gotoWar");
                    casinoWarDealer casinodealer = (casinoWarDealer)dealer;
                    casinodealer.resolveWar(gotoWar, additionalBet);
                    break;
                case "leave":
                    leave(player);
                    break;
                default:
                    player.sendMessage(mg.errorMessage("unknown meesage type").toString());
            }
        }catch(Exception e){
            e.printStackTrace();
            player.sendMessage(mg.errorMessage("unknown meesage type").toString());
        }
    }
}
