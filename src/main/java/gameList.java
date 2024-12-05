import java.util.ArrayList;
import java.util.List;

public class gameList {
    private List<game> list;
    private messageGenerator mg;
    private gameList(){}
    public static gameList getInstance(){
        return Holder.instance;
    }
    public static class Holder{
        private static final gameList instance = new gameList();
    }
    public void init(messageGenerator mg){
        this.mg = mg;
        list = new ArrayList<>();
        for(int i=0; i<3; i++){
            list.add(new game(mg, i));
        }
    }
    public game getGameInstance(int game){
        return list.get(game);
    }
    public int numberOfGames(){
        return list.size();
    }
}
