import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class blackJackDealer implements dealerI{
    private room room;
    private messageGenerator mg;
    private Deck deck = new Deck();
    private List<Card> dealerCards = new CopyOnWriteArrayList<>();
    private volatile Map<String, List<Card>> playerHands = new ConcurrentHashMap<String, List<Card>>();
    private volatile Map<String, Integer> currentBets = new ConcurrentHashMap<>();
    private AtomicBoolean playerAct = new AtomicBoolean(false);
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

    public void play(List<client> players, Map<String, Boolean> activePlayers, int numberOfActivePlayer){
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
            playerHands.put(player.getName(), hand);
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
        int playerValue = getHandValue(playerHands.get(playerTurn.getName()));
        int dealerValue = getHandValue(dealerCards);
        String result;
        if(action.equals("hit")){
            playerHands.get(playerTurn.getName()).add(deck.drawCard());
            int handValue = getHandValue(playerHands.get(playerTurn.getName()));
            if(handValue>21){
                result = "bust";
                playerTurn.sendMessage(mg.gameResult(0, result, playerHands.get(playerTurn.getName()), dealerCards).toString());
                return;
            }
            playerValue = getHandValue(playerHands.get(playerTurn.getName()));
            dealerValue = getHandValue(dealerCards);
        }else if(action.equals("stand")) {
            while (getHandValue(dealerCards) < 17) {
                dealerCards.add(deck.drawCard());
            }
        }
        if(dealerValue>21 || playerValue>dealerValue){
            result = "win";
            playerTurn.getUserInstance().addMoney(currentBets.get(playerTurn.getName())*2);
        }else if(playerValue==dealerValue){
            result = "push";
            playerTurn.getUserInstance().addMoney(currentBets.get(playerTurn.getName()));
        }else{
            result = "lose";
        }
        playerAct.set(true);
        playerTurn.sendMessage(mg.gameResult(playerTurn.getUserInstance().getMoney(), result, playerHands.get(playerTurn.getName()), dealerCards).toString());
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
