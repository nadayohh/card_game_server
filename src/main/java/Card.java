public class Card {
    private final String suit;
    private final int rank;

    public Card(String suit, int rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public String getSuit() {
        return suit;
    }

    public int getRank() {
        return rank;
    }
    public int getValue() {
        if(rank==1) return 11;
        else if(rank==10||rank==11||rank==12)return 10;
        else{
            return rank;
        }

    }

    @Override
    public String toString() {
        return suit + rank;
    }
}