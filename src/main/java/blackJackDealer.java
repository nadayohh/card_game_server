import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.*;

public class blackJackDealer implements dealerI{
    private room room;
    private messageGenerator mg;
    private Deck deck = new Deck();
    private List<Card> dealerCards = new ArrayList<>();
    private Map<client, List<Card>> playerHands = new HashMap<>();
    private Map<client, Integer> currentBets = new HashMap<>();
    private volatile boolean playerAct = false;
    private volatile client playerTurn;
    private ScheduledExecutorService timerExecutor;
    private volatile int roundTime = 30;
    private CountDownLatch counter;

    public blackJackDealer(room room, messageGenerator mg) {
        this.room = room;
        this.mg = mg;
    }

    public client checkPlayerTurn(){
        return this.playerTurn;
    }

    public void play(List<client> players, Map<client, Boolean> activePlayers, int numberOfActivePlayer){
        timerExecutor = Executors.newScheduledThreadPool(1);
        deck.shuffle();
        waitForAct(players, activePlayers);
        dealInitialCards(players);
        waitForAct(players, activePlayers);
        timerExecutor.shutdown();
    }

    private void dealInitialCards(List<client> players){
        dealerCards.add(deck.drawCard());
        dealerCards.add(deck.drawCard());
        for(client player : players){
            List<Card> hand = new ArrayList<>();
            hand.add(deck.drawCard());
            hand.add(deck.drawCard());
            playerHands.put(player, hand);
            player.sendMessage(mg.blackJackCard(hand, dealerCards).toString());
        }
    }
    private int getHandValue(List<Card> hand) {
        int value = 0;
        int aceCount = 0;
        for (Card card : hand) {
            value += card.getValue();
            if (card.getRank() == 1) {
                aceCount++;
            }
        }
        while (value > 21 && aceCount > 0) {
            value -= 10;
            aceCount--;
        }
        return value;
    }

    public void handleBet(client player, int amount, String bet){
        if(player.getUserInstance().getMoney()<amount){
            player.sendMessage(mg.errorMessage("잔액 부족").toString());
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

    public void playRounds(client player, String action){
        int playerValue = getHandValue(playerHands.get(player));
        int dealerValue = getHandValue(dealerCards);
        String result;
        if(action.equals("hit")){
            playerHands.get(player).add(deck.drawCard());
            int handValue = getHandValue(playerHands.get(player));
            if(handValue>21){
                result = "bust";
                player.sendMessage(mg.gameResult(0, result, playerHands.get(player), dealerCards).toString());
                return;
            }
            playerValue = getHandValue(playerHands.get(player));
            dealerValue = getHandValue(dealerCards);
        }else if(action.equals("stand")) {
            while (getHandValue(dealerCards) < 17) {
                dealerCards.add(deck.drawCard());
            }
        }
        if(dealerValue>21 || playerValue>dealerValue){
            result = "win";
            player.getUserInstance().addMoney(currentBets.get(player)*2);
        }else if(playerValue==dealerValue){
            result = "push";
            player.getUserInstance().addMoney(currentBets.get(player));
        }else{
            result = "lose";
        }
        player.sendMessage(mg.gameResult(player.getUserInstance().getMoney(), result, playerHands.get(player), dealerCards).toString());
        playerAct = true;
    }

    private synchronized void waitForAct(List<client> players, Map<client, Boolean> activePlayers){
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
