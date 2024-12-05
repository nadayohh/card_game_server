import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class casinoWarDealer implements dealerI {
    private room room;
    private Deck deck = new Deck();
    private final List<Card> dealerCards = new CopyOnWriteArrayList<>();
    private final Map<String, List<Card>> playerHands = new ConcurrentHashMap<>();
    private final Map<String, Integer> currentBets = new ConcurrentHashMap<>();
    private final AtomicBoolean playerAct = new AtomicBoolean(false);
    private volatile client playerTurn;
    private ScheduledExecutorService timerExecutor;
    private int roundTime = 30;
    private CountDownLatch counter;
    private messageGenerator mg;

    public casinoWarDealer(room room, messageGenerator mg){
        this.room = room;
        this.mg = mg;
    }

    public client checkPlayerTurn(){
        return this.playerTurn;
    }

    public void play(List<client> players, Map<String, Boolean> activePlayers, int numberOfActivePlayer){
        timerExecutor = Executors.newScheduledThreadPool(1);
        deck = new Deck();
        deck.shuffle();
        waitForAct(players, activePlayers);
        dealInitialCards(players);
        waitForAct(players, activePlayers);
        timerExecutor.shutdown();
    }
    private synchronized void dealInitialCards(List<client> players){
        dealerCards.add(deck.drawCard());
        for(client player : players){
            List<Card> hand = new ArrayList<>();
            hand.add(deck.drawCard());
            playerHands.put(player.getName(),hand);
            player.sendMessage(mg.casinoWarCard(playerHands.get(player.getName())).toString());
        }
    }

    public synchronized void playRounds(String action){
        int prize;
        String result;
        if(playerHands.get(playerTurn.getName()).getFirst().getRank()> dealerCards.getFirst().getRank()){
            result = "win";
            prize = currentBets.get(playerTurn.getName())*2;
            playerTurn.getUserInstance().addMoney(currentBets.get(playerTurn.getName())*2);
            playerTurn.sendMessage(mg.gameResult(prize, result, playerHands.get(playerTurn.getName()), dealerCards).toString());
            playerAct.set(true);
        }else if(playerHands.get(playerTurn.getName()).getFirst().getRank()> dealerCards.getFirst().getRank()) {
            result = "lose";
            prize = 0;
            playerTurn.sendMessage(mg.gameResult(prize, result, playerHands.get(playerTurn.getName()), dealerCards).toString());
            playerAct.set(true);
        }else {
            result = "war";
            prize = 0;
            playerTurn.sendMessage(mg.gameResult(prize, result,playerHands.get(playerTurn.getName()), dealerCards).toString());
        }
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        response.put("response", "gameResult");
        data.put("result", result)
                .put("playerCards", playerHands.get(playerTurn.getName()))
                .put("dealerCards", dealerCards)
                .put("prize", prize)
                .put("player", playerTurn.getName());
        response.put("data", data);
        room.broadcast(response.toString());
    }

    public void resolveWar(String gotoWar, int additionalBet){
        int totalBet = currentBets.get(playerTurn.getName());
        String result;
        int prize;
        if(gotoWar.equals("true")){
            totalBet += additionalBet;
            playerHands.get(playerTurn.getName()).add(deck.drawCard());
            dealerCards.add(deck.drawCard());
            if(playerHands.get(playerTurn.getName()).getLast().getRank()> dealerCards.getLast().getRank()){
                result = "win";
                prize = totalBet*2;
                playerTurn.getUserInstance().addMoney(prize);
            }else if(playerHands.get(playerTurn.getName()).getLast().getRank()> dealerCards.getLast().getRank()){
                result = "lose";
                prize = 0;
            }else{
                result = "lose";
                prize = totalBet;
                playerTurn.getUserInstance().addMoney(totalBet);
            }
        }else{
            result = "surrender";
            prize = totalBet/2;
        }
        playerTurn.sendMessage(mg.gameResult(prize, result, playerHands.get(playerTurn.getName()), dealerCards).toString());
        playerAct.set(true);
    }

    public void handleBet(int amount, String bet){
        if(playerTurn.getUserInstance().getMoney()<amount){
            playerTurn.sendMessage(mg.errorMessage("not enough money").toString());
            return;
        }
        playerTurn.getUserInstance().betMoney(amount);
        currentBets.put(playerTurn.getName(), amount);
        room.broadcastGameUpdate(playerTurn.getName(), amount);
        playerAct.set(true);
    }

    private void handleTimeouts(client player, Map<String, Boolean> activePlayers){
        activePlayers.remove(player.getName());
        activePlayers.put(player.getName(),false);
        player.sendMessage(mg.errorMessage("timeout").toString());
    }

    private void waitForAct(List<client> players, Map<String, Boolean> activePlayers){
        for(client player : players){
            if(!activePlayers.get(player.getName())) continue;
            playerTurn = player;
            this.counter = new CountDownLatch(1);
            ScheduledFuture<?> future = timerExecutor.scheduleAtFixedRate(()->{
                if(roundTime > 0 && !playerAct.get()){
                    room.broadcastTimer(roundTime);
                    roundTime--;
                }else{
                    if(roundTime<=0) handleTimeouts(player, activePlayers);
                    roundTime = 30;
                    playerAct.set(false);
                    room.broadcastTimer(roundTime);
                    counter.countDown();
                }
            },0,1, TimeUnit.SECONDS);
            try{
                counter.await();
                future.cancel(true);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
