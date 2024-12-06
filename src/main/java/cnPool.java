import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;

public class cnPool {
    private String url = "jdbc:mysql://chabin37.iptime.org:3306/cardmaster_db";
    private String user = "cardmaster10";
    private String pwd = "password";
    static final int INITIAL_CAPACITY = 10;
    LinkedList<Connection> pool = new LinkedList<Connection>();

    private cnPool() {
        try {
            for (int i = 0; i < INITIAL_CAPACITY; i++) {
                pool.add(DriverManager.getConnection(url, user, pwd));
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public static cnPool getInstance(){
        return Holder.instance;
    }

    public static class Holder{
        private static final cnPool instance = new cnPool();
    }

    public synchronized Connection getConnection() {
        if(pool.isEmpty()){
            try {
                pool.add(DriverManager.getConnection(url, user, pwd));
            }catch(SQLException e){
                e.printStackTrace();
            }
        }
        return pool.pop();
    }

    public synchronized void returnConnection(Connection connection){
        pool.push(connection);
    }
}
