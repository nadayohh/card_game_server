import java.util.*;

class PokerHandEvaluator {

    public PokerHand evaluate(List<Card> cards) {
        if (cards.size() < 5) {
            throw new IllegalArgumentException("핸드는 최소 5장의 카드로 구성되어야 합니다.");
        }

        List<Card> sortedCards = new ArrayList<>(cards);
        sortedCards.sort(Comparator.comparingInt(Card::getRank).reversed()); // 높은 숫자 순 정렬

        boolean isFlush = checkFlush(sortedCards);
        boolean isStraight = checkStraight(sortedCards);
        Map<Integer, Integer> rankCounts = getRankCounts(sortedCards);

        if (isFlush && isStraight) {
            return new PokerHand("Straight Flush", sortedCards);
        }

        if (rankCounts.containsValue(4)) {
            return new PokerHand("Four of a Kind", sortedCards);
        }

        if (rankCounts.containsValue(3) && rankCounts.containsValue(2)) {
            return new PokerHand("Full House", sortedCards);
        }

        if (isFlush) {
            return new PokerHand("Flush", sortedCards);
        }

        if (isStraight) {
            return new PokerHand("Straight", sortedCards);
        }

        if (rankCounts.containsValue(3)) {
            return new PokerHand("Three of a Kind", sortedCards);
        }

        if (Collections.frequency(rankCounts.values(), 2) == 2) {
            return new PokerHand("Two Pair", sortedCards);
        }

        if (rankCounts.containsValue(2)) {
            return new PokerHand("One Pair", sortedCards);
        }

        return new PokerHand("High Card", sortedCards);
    }

    private boolean checkFlush(List<Card> cards) {
        String suit = cards.get(0).getSuit();
        for (Card card : cards) {
            if (!card.getSuit().equals(suit)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkStraight(List<Card> cards) {
        List<Integer> ranks = new ArrayList<>();
        for (Card card : cards) {
            ranks.add(card.getRank());
        }

        // 에이스는 1로도, 14로도 계산 가능
        if (ranks.contains(14)) {
            ranks.add(1);
        }

        Collections.sort(ranks);

        int consecutiveCount = 1;
        for (int i = 1; i < ranks.size(); i++) {
            if (ranks.get(i) == ranks.get(i - 1) + 1) {
                consecutiveCount++;
                if (consecutiveCount >= 5) {
                    return true;
                }
            } else if (ranks.get(i) != ranks.get(i - 1)) {
                consecutiveCount = 1;
            }
        }

        return false;
    }

    private Map<Integer, Integer> getRankCounts(List<Card> cards) {
        Map<Integer, Integer> rankCounts = new HashMap<>();
        for (Card card : cards) {
            rankCounts.put(card.getRank(), rankCounts.getOrDefault(card.getRank(), 0) + 1);
        }
        return rankCounts;
    }
}

class PokerHand implements Comparable<PokerHand> {
    private final String handType;
    private final List<Card> cards;

    public PokerHand(String handType, List<Card> cards) {
        this.handType = handType;
        this.cards = new ArrayList<>(cards);
    }

    public String getHandType() {
        return handType;
    }

    public List<Card> getCards() {
        return cards;
    }

    @Override
    public String toString() {
        return handType + " " + cards;
    }

    @Override
    public int compareTo(PokerHand other) {
        // 핸드 랭킹에 따른 비교 (텍사스 홀덤 핸드 순위)
        Map<String, Integer> handRankings = Map.of(
                "High Card", 1,
                "One Pair", 2,
                "Two Pair", 3,
                "Three of a Kind", 4,
                "Straight", 5,
                "Flush", 6,
                "Full House", 7,
                "Four of a Kind", 8,
                "Straight Flush", 9
        );

        int thisRank = handRankings.getOrDefault(this.handType, 0);
        int otherRank = handRankings.getOrDefault(other.handType, 0);

        if (thisRank != otherRank) {
            return Integer.compare(thisRank, otherRank);
        }

        // 같은 핸드일 경우 카드 숫자로 비교
        for (int i = 0; i < Math.min(this.cards.size(), other.cards.size()); i++) {
            int compare = Integer.compare(this.cards.get(i).getRank(), other.cards.get(i).getRank());
            if (compare != 0) {
                return compare;
            }
        }

        return 0;
    }
}
