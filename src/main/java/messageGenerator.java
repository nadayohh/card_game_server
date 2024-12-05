import org.json.JSONObject;

import java.util.List;

public class messageGenerator {
    JSONObject response;
    JSONObject data;
    public messageGenerator(){}

    public JSONObject loginSuccess(gameList games){
        response = new JSONObject();
        data = new JSONObject();
        response.put("response", "loginSuccess");
        data.put("numberOfGame",games.numberOfGames());
        response.put("data", data);
        return response;
    }
    public JSONObject sendRoomList(game game){
        response = new JSONObject();
        data = new JSONObject();
        response.put("response", "roomList");
        data.put("numberOfRoom", game.numberOfRoom());
        for(int i=0; i<game.numberOfRoom(); i++){
            data.put("numberOfPlayer", new JSONObject()
                    .put("room"+i, game.numberOfPlayer(i)));
        }
        for(int i=0; i<game.numberOfRoom(); i++){
            data.put("roomName", new JSONObject()
                    .put("room"+i, game.getRoomName(i)));
        }
        response.put("data", data);
        return response;
    }
    public JSONObject updateRoomState(room room){
        response = new JSONObject();
        data = new JSONObject();
        response.put("response", "roomStateUpdate");
        data.put("numberOfPlayer", room.numberOfPlayer());
        response.put("data", data);
        return response;
    }
    public JSONObject registerSuccess(){
        response = new JSONObject();
        data = new JSONObject();
        response.put("response", "registerSuccess");
        data.put("message", "register Success");
        response.put("data", data);
        return response;
    }
    public JSONObject updatePlayerState(boolean state){
        response = new JSONObject();
        data = new JSONObject();
        response.put("response", "enterSuccess");
        data.put("playing", state);
        response.put("data", data);
        return response;
    }
    public JSONObject errorMessage(String message){
        response = new JSONObject();
        data = new JSONObject();
        response.put("response", "error");
        data.put("message", message);
        response.put("data", data);
        return response;
    }
    public JSONObject gameUpdate(String playerTurn, int currentBet){
        response = new JSONObject();
        data = new JSONObject();
        response.put("response", "blackJackCard");
        data.put("playerTurn", playerTurn);
        data.put("currentBet", currentBet);
        response.put("data", data);
        return response;
    }
    public JSONObject blackJackCard(List<Card> playerHands, List<Card> dealerCards){
        response = new JSONObject();
        data = new JSONObject();
        response.put("response", "blackJackCard");
        data.put("playerCard", playerHands)
                .put("dealerCard", dealerCards.getFirst());
        response.put("data", data);
        return response;
    }
    public JSONObject updateHand(List<Card> playerHands){
        response = new JSONObject();
        data = new JSONObject();
        response.put("response", "updateHand");
        data.put("playerCard", playerHands);
        response.put("data", data);
        return response;
    }
    public JSONObject casinoWarCard(List<Card> playerHand){
        response = new JSONObject();
        data = new JSONObject();
        response.put("response", "casinoWarCard");
        data.put("playerCard", playerHand);
        response.put("data", data);
        return response;
    }
    public JSONObject gameResult(int amount, String result, List<Card> playerCards, List<Card> dealerCards){
        response = new JSONObject();
        data = new JSONObject();
        response.put("response", "gameResult");
        data.put("result", result)
                .put("playerCards", playerCards)
                .put("dealerCards", dealerCards)
                .put("prize", amount);
        response.put("data", data);
        return response;
    }
}
