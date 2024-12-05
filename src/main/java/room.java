import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class room extends Thread{
    private String roomName;
    private volatile List<client> players = new ArrayList<>();
    private volatile Map<client, Boolean> activePlayers = new HashMap<>();
    private volatile client leader;
    private dealerI dealer;
    private int maxPlayer = 8;
    private boolean gameInProgress = false;
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
            if(gameId==0){
                dealer = new blackJackDealer(this, mg);
            }else if(gameId==1){
                dealer = new casinoWarDealer(this, mg);
            }else{
                dealer = new baccaratDealer(this, mg);
            }
        }
        broadcast(mg.updateRoomState(this).toString());
        if(numberOfPlayer() > maxPlayer){
            client.sendMessage(mg.errorMessage("enter Failed").toString());
        }
        if(gameInProgress){
            players.add(client);
            activePlayers.put(client, false);
        }else{
            players.add(client);
            activePlayers.put(client, true);
        }
        client.sendMessage(mg.updatePlayerState(activePlayers.get(client)).toString());
    }

    public void leave(client player){
        players.remove(player);
        activePlayers.remove(player);
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
        broadcast(""+numberOfPlayer());
        gameInProgress = true;
        dealer.play(players, activePlayers, numberOfActivePlayer());
        gameInProgress = false;
    }

    public boolean isLeader(client player){
        return leader == player;
    }

    private int numberOfActivePlayer(){
        int tmp = 0;
        for(client player : players){
            if(activePlayers.get(player))tmp++;
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
                    if(dealer.checkPlayerTurn()!=player){
                        player.sendMessage(mg.errorMessage("not your turn").toString());
                        return;
                    }
                    int amount = data.optInt("amount", 0);
                    String bet = data.getString("bet");
                    dealer.handleBet(player, amount, bet);
                    break;
                case "hitOrStand":
                    if(dealer.checkPlayerTurn()!=player){
                        player.sendMessage(mg.errorMessage("not your turn").toString());
                        return;
                    }
                    String hitOrStand = data.getString("hitOrStand");
                    dealer.playRounds(player, hitOrStand);
                    break;
                case "call":
                    int additionalBet = data.optInt("amount", 0);
                    String gotoWar = data.getString("gotoWar");
                    casinoWarDealer casinodealer = (casinoWarDealer)dealer;
                    casinodealer.resolveWar(player, gotoWar, additionalBet);
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
