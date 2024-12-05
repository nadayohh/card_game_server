import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class casinoWarDealer implements dealerI {
    private room room;
    private Deck deck = new Deck();
    private List<Card> dealerCards = new ArrayList<>();
    private Map<client, List<Card>> playerHands = new HashMap<>();
    private Map<client, Integer> currentBets = new HashMap<>();
    private Map<client, Boolean> warOccur = new HashMap<>();
    private boolean playerAct = false;
    private client playerTurn;
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

    public void play(List<client> players, Map<client, Boolean> activePlayers, int numberOfActivePlayer){
        timerExecutor = Executors.newScheduledThreadPool(1);
        deck = new Deck();
        deck.shuffle();
        waitForAct(players, activePlayers);
        dealInitialCards(players);
        waitForAct(players, activePlayers);
        timerExecutor.shutdown();
    }
    private void dealInitialCards(List<client> players){
        dealerCards.add(deck.drawCard());
        for(client player : players){
            List<Card> hand = new ArrayList<>();
            hand.add(deck.drawCard());
            playerHands.put(player,hand);
            player.sendMessage(mg.casinoWarCard(playerHands.get(player)).toString());
        }
    }

    public void playRounds(client player, String action){
        int prize;
        String result;
        if(playerHands.get(player).getFirst().getRank()> dealerCards.getFirst().getRank()){
            result = "win";
            prize = currentBets.get(player)*2;
            player.getUserInstance().addMoney(currentBets.get(player)*2);
            player.sendMessage(mg.gameResult(prize, result, playerHands.get(player), dealerCards).toString());
            warOccur.put(player, false);
        }else if(playerHands.get(player).getFirst().getRank()> dealerCards.getFirst().getRank()) {
            result = "lose";
            prize = 0;
            player.sendMessage(mg.gameResult(prize, result, playerHands.get(player), dealerCards).toString());
            warOccur.put(player, false);
        }else {
            result = "war";
            prize = 0;
            player.sendMessage(mg.gameResult(prize, result, playerHands.get(player), dealerCards).toString());
            warOccur.put(player, true);
        }
    }

    public void resolveWar(client player, String gotoWar, int additionalBet){
        int totalBet = currentBets.get(player);
        String result;
        int prize;
        if(gotoWar.equals("true")){
            totalBet += additionalBet;
            playerHands.get(player).add(deck.drawCard());
            dealerCards.add(deck.drawCard());
            if(playerHands.get(player).getLast().getRank()> dealerCards.getLast().getRank()){
                result = "win";
                prize = totalBet*2;
                player.getUserInstance().addMoney(prize);
            }else if(playerHands.get(player).getLast().getRank()> dealerCards.getLast().getRank()){
                result = "lose";
                prize = 0;
            }else{
                result = "lose";
                prize = totalBet;
                player.getUserInstance().addMoney(totalBet);
            }
        }else{
            result = "surrender";
            prize = totalBet/2;
        }
        player.sendMessage(mg.gameResult(prize, result, playerHands.get(player), dealerCards).toString());
    }

    public void handleBet(client player, int amount, String bet){
        if(player.getUserInstance().getMoney()<amount){
            player.sendMessage(mg.errorMessage("not enough money").toString());
            return;
        }
        player.getUserInstance().betMoney(amount);
        currentBets.put(player, amount);
        room.broadcastGameUpdate(player.getName(), amount);
        playerAct = true;
    }

    private void handleTimeouts(client player, Map<client, Boolean> activePlayers){
        activePlayers.remove(player);
        activePlayers.put(player,false);
        player.sendMessage(mg.errorMessage("timeout").toString());
    }

    private void waitForAct(List<client> players, Map<client, Boolean> activePlayers){
        for(client player : players){
            if(!activePlayers.get(player)) continue;
            playerTurn = player;
            this.counter = new CountDownLatch(1);
            ScheduledFuture<?> future = timerExecutor.scheduleAtFixedRate(()->{
                if(roundTime > 0 && !playerAct){
                    room.broadcastTimer(roundTime);
                    roundTime--;
                }else{
                    if(roundTime<=0) handleTimeouts(player, activePlayers);
                    roundTime = 30;
                    playerAct = false;
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
