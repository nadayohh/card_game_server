import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        String[] suits = {"♠", "♥", "♦", "♣"};
        for (String suit : suits) {
            for (int rank = 1; rank <= 13; rank++) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card drawCard() {
        if (cards.isEmpty()) throw new IllegalStateException("덱에 카드가 없습니다!");
        return cards.remove(cards.size() - 1);
    }

    public void reset() {
        cards.clear();
        String[] suits = {"♠", "♥", "♦", "♣"};
        for (String suit : suits) {
            for (int rank = 1; rank <= 13; rank++) {
                cards.add(new Card(suit, rank));
            }
        }
    }
}