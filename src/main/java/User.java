public class User {
    private String id;
    private int money;
    public User(String id, int money){
        this.id = id;
        this.money = money;
    }
    public String getId(){return this.id;}
    public int getMoney(){return this.money;}
    public void addMoney(int amount){
        this.money += amount;
    }
    public void betMoney(int amount) {
        if (this.money < amount) {
            throw new IllegalStateException("돈이 부족합니다.");
        }
        this.money -= amount;
    }
}