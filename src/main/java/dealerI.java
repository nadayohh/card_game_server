import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public interface dealerI {
    public void play(List<client> players, Map<String, Boolean> activePlayers, int numberOfActivePlayer);
    public void playRounds(String action);
    public client checkPlayerTurn();
    public void handleBet(int amount, String bet);
}
