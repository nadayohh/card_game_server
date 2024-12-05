import com.mysql.cj.protocol.Resultset;

import java.sql.*;
import java.util.LinkedList;

public class userManager {
    public userManager(){}

    public synchronized User UserExist(String id, String pwd) throws SQLException {
        cnPool pool = cnPool.getInstance();
        Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("select * from users where id = ? and pwd = ?");
        statement.setString(1, id);
        statement.setString(2, pwd);
        ResultSet resultSet = statement.executeQuery();
        if(!resultSet.next()){
            resultSet.close();
            statement.close();
            pool.returnConnection(connection);
            return null;
        }else{
            User User = new User(resultSet.getString("id"), resultSet.getInt("money"));
            resultSet.close();
            statement.close();
            pool.returnConnection(connection);
            return User;
        }
    }

    public synchronized boolean idExist(String id) throws SQLException{
        cnPool pool = cnPool.getInstance();
        Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("select * from users where id = ?");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        if(resultSet.next()){
            resultSet.close();
            statement.close();
            pool.returnConnection(connection);
            return true;
        }else{
            resultSet.close();
            statement.close();
            pool.returnConnection(connection);
            return false;
        }
    }

    public synchronized boolean createUser(String id, String pwd) throws SQLException{
        cnPool pool = cnPool.getInstance();
        Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("insert into users(id, pwd, money) values (?,?,500)");
        statement.setString(1, id);
        statement.setString(2, pwd);
        int res = statement.executeUpdate();
        System.out.println(res);
        if(res>0){
            statement.close();
            pool.returnConnection(connection);
            return true;
        }else{
            statement.close();
            pool.returnConnection(connection);
            return false;
        }
    }

    public synchronized boolean deleteUser(String id) throws SQLException{
        cnPool pool = cnPool.getInstance();
        Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("delete from users where id = ?");
        statement.setString(1, id);
        int res = statement.executeUpdate();
        if(res>0){
            statement.close();
            pool.returnConnection(connection);
            return true;
        }else{
            statement.close();
            pool.returnConnection(connection);
            return false;
        }
    }
}