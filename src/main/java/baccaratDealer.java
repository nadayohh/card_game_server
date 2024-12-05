import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class baccaratDealer implements dealerI{
    private room room;
    private Deck deck = new Deck();
    private ArrayList<Card> dealerCards = new ArrayList<>();
    private Map<client, List<Card>> playerHands = new HashMap<>();
    private Map<client, Boolean> activePlayers = new HashMap<>();
    private Map<client, String> betting = new HashMap<>();
    private Map<client, Integer> currentBets = new HashMap<>();
    private boolean playerAct = false;
    private client playerTurn;
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

    public void play(List<client> players, Map<client, Boolean> activePlayers, int numberOfActivePlayer){
        timerExecutor = Executors.newScheduledThreadPool(1);
        deck.shuffle();
        waitForAct(players, activePlayers);
        dealInitialCards(players);
        for(client player : players){
            if(!activePlayers.get(player)) continue;
            playRounds(player, "");
        }
    }

    private void dealInitialCards(List<client> players){
        dealerCards.add(deck.drawCard());
        dealerCards.add(deck.drawCard());
        for(client player : players){
            if(!activePlayers.get(player)) continue;
            List<Card> hand = new ArrayList<>();
            hand.add(deck.drawCard());
            hand.add(deck.drawCard());
            playerHands.put(player, hand);
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
        int playerScore = calculateScore(playerHands.get(player));
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
        if(betting.get(player) == result){
            if(result=="Tie"){
                prize = currentBets.get(player)*8;
                player.getUserInstance().addMoney(currentBets.get(player)*8);
            }else{
                prize = currentBets.get(player)*2;
                player.getUserInstance().addMoney(currentBets.get(player)*2);
            }
        }
        player.sendMessage(mg.gameResult(prize, result, playerHands.get(player), dealerCards).toString());
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
