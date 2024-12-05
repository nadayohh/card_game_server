import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class baccaratDealer implements dealerI{
    private room room;
    private Deck deck = new Deck();
    private final List<Card> dealerCards = new CopyOnWriteArrayList<>();
    private volatile Map<String, List<Card>> playerHands = new ConcurrentHashMap<>();
    private volatile Map<String, String> betting = new ConcurrentHashMap<>();
    private volatile Map<String, Integer> currentBets = new ConcurrentHashMap<>();
    private AtomicBoolean playerAct = new AtomicBoolean(false);
    private volatile client playerTurn;
    private ScheduledExecutorService timerExecutor;
    private int roundTime = 30;
    private CountDownLatch counter;
    private messageGenerator mg;

    public baccaratDealer(room room, messageGenerator mg){
        this.room = room;
        this.mg = mg;
    }

    public client checkPlayerTurn(){
        return this.playerTurn;
    }

    public void play(List<client> players, Map<String, Boolean> activePlayers, int numberOfActivePlayer){
        timerExecutor = Executors.newScheduledThreadPool(1);
        deck.shuffle();
        waitForAct(players, activePlayers);
        dealInitialCards(players, activePlayers);
        for(client player : players){
            playerTurn = player;
            if(!activePlayers.get(player.getName())) continue;
            playRounds("");
        }
    }

    private void dealInitialCards(List<client> players, Map<String, Boolean> activePlayers){
        dealerCards.add(deck.drawCard());
        dealerCards.add(deck.drawCard());
        for(client player : players){
            if(!activePlayers.get(player.getName())) continue;
            List<Card> hand = new ArrayList<>();
            hand.add(deck.drawCard());
            hand.add(deck.drawCard());
            playerHands.put(player.getName(), hand);
            player.sendMessage(mg.blackJackCard(hand, dealerCards).toString());
        }
    }

    private int calculateScore(List<Card> cards){
        int score = 0;
        for (Card card : cards) {
            if (card.getRank()>=10) {
                score += 0;
            } else if (card.getRank()==1) {
                score += 1;
            } else {
                score += card.getRank();
            }
        }
        return score % 10;
    }

    public void handleBet(int amount, String bet){
        if(playerTurn.getUserInstance().getMoney()<amount){
            playerTurn.sendMessage(mg.errorMessage("잔액 부족").toString());
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

    public void playRounds(String action){
        int playerScore = calculateScore(playerHands.get(playerTurn.getName()));
        int bankerScore = calculateScore(dealerCards);
        String result;
        int prize = 0;
        if(playerScore == bankerScore){
            result = "Tie";
        }else if(playerScore>bankerScore){
            result = "Player";
        }else{
            result = "Banker";
        }
        if(Objects.equals(betting.get(playerTurn.getName()), result)){
            if(result=="Tie"){
                prize = currentBets.get(playerTurn.getName())*8;
                playerTurn.getUserInstance().addMoney(currentBets.get(playerTurn.getName())*8);
            }else{
                prize = currentBets.get(playerTurn.getName())*2;
                playerTurn.getUserInstance().addMoney(currentBets.get(playerTurn.getName())*2);
            }
        }
        playerTurn.sendMessage(mg.gameResult(prize, result, playerHands.get(playerTurn.getName()), dealerCards).toString());
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
