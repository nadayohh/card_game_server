import java.util.*;

public class game {
    List<room> list;
    int gameId;

    public game(messageGenerator mg, int gameId) {
        list = new ArrayList<>();
        this.gameId = gameId;
        list.add(new room("defaultRoom", mg, gameId));
    }

    public synchronized int numberOfRoom() {
        return list.size();
    }

    public synchronized String getRoomName(int idx) {
        return list.get(idx).getRoomName();
    }

    public synchronized int numberOfPlayer(int idx) {
        return list.get(idx).numberOfPlayer();
    }

    public synchronized room getRoomInstance(int idx){
        return list.get(idx);
    }

    public synchronized room enterRoom(int idx, client thread) {
        list.get(idx).join(thread);
        return list.get(idx);
    }

    public synchronized room quitRoom(int idx, client thread) {
        list.get(idx).leave(thread);
        return list.get(idx);
    }

    public synchronized void makeRoom(String roomName, messageGenerator mg, int gameId) {
        list.add(new room(roomName, mg, gameId));
    }
}