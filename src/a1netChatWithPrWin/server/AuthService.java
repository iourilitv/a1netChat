package a1netChatWithPrWin.server;

import java.sql.*;

/**
 * Класс для организации сервиса авторизации и связи с БД
 * Связь БД и приложения осуществляется через посредника, JDBC драйвер(библиотека).
 * Home work for lesson 2.
 * @author Litvinenko Yuriy.
 */
public class AuthService {

    //объект для установления связи
    private static Connection connection;
    //объект для отправки запросов в JDBC драйвер(библиотека) с помощью метода connect(),
    // который переправляет его в БД.
    // И получает результат (объект класса ResultSet) с помощью executeQuery(sql)
    private static Statement stmt;

    /**
     * Метод подключения к БД
     * //@throws SQLException
     */
    public static void connect() throws SQLException {
        try {
            // обращение к драйверу. просто инициализирует класс, с которым потом будем работать
            Class.forName("org.sqlite.JDBC");
            // установка подключения
            connection = DriverManager.getConnection("jdbc:sqlite:mainDB.db");
            // создание Statement для возможности отправки запросов
            stmt = connection.createStatement();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод запрашивающий в БД nickname по совпадению логина и пароля.
     * @param login - логин
     * @param pass - пароль
     * @return значение колонки nickname, если сопадение
     */
    public static String getNickByLoginAndPass(String login, String pass) {
        // формирование запроса. '%s' - для последовательного подставления значений в соотвествующее место
        String sql = String.format("SELECT nickname FROM main where login = '%s' and password = '%s'", login, pass);

        try {
            // оправка запроса и получение ответа из БД
            ResultSet rs = stmt.executeQuery(sql);

            // если есть строка возвращаем результат, если нет, то вернеться null
            if(rs.next()) {
                //индекс колонки в запросе (здесь 1 - это nickname). Но индексация в БД начинается с 1
                //можно также вызвать и по columnLabel (здесь было бы "nickname"), но по индексу быстрее
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Метод проверки введенных логина или ника в БД на уникальность
    public static boolean checkLoginAndNicknameInDB(String[] tokens) {
        String login = tokens[2];
        String nickname = tokens[1];

        // формирование запроса. '%s' - для последовательного подставления значений в соотвествующее место
        String sql = String.format("SELECT id FROM main where login = '%s' or nickname = '%s'", login, nickname);

        try {
            // оправка запроса и получение ответа из БД
            ResultSet rs = stmt.executeQuery(sql);

            // если есть строка возвращаем результат, если нет, то вернеться null
            if(!rs.next()) {
                //таких логина или ник в БД нет
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Метод добавления данных пользователя в БД
    public static boolean addUserIntoDB(String[] tokens){
        String login = tokens[2];
        String password = tokens[3];
        String nickname = tokens[1];

        // формирование запроса. '%s' - для последовательного подставления значений в соотвествующее место
        //записываем данные нового юзера в БД
        String sql = String.format("INSERT INTO main (login, password, nickname) VALUES ('%s', '%s', '%s')", login, password, nickname);

            try {
                // оправка запроса и получение ответа из БД
                int rs = stmt.executeUpdate(sql);

                // если строка добавлена, то возвращается 1, если нет, то вернеться 0?
                if(rs != 0) {
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        return false;
    }

    //Метод проверки есть ли уже таблица черного списка у пользователя в БД
    //если есть строка возвращаем результат, если нет, то вернеться null
    public static String getUserBlacklistNameByNicknameInDB(String nickOfOwner) {
        // формирование запроса. '%s' - для последовательного подставления значений в соотвествующее место
        String sql = String.format("SELECT name_blacklists FROM main where nickname = '%s'", nickOfOwner);
        try {
            // оправка запроса и получение ответа из БД
            ResultSet rs = stmt.executeQuery(sql);
            // если есть строка возвращаем результат, если нет, то вернеться null
            if(rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Метод создания в БД таблицы с черным списком пользователя
    //если таблица создана успешно, возвращает true
    public static boolean createUserBlacklistInDB(String nickOfOwner){
        //формируем имя таблицы с черным списком пользователя
        String nameOfBlacklist = nickOfOwner + "blacklist";
        //создаем новую таблицу для черного списка пользователя
        // формирование запроса. '%s' - для последовательного подставления значений в соотвествующее место
        String sql1 = String.format("CREATE TABLE %s ( nickname REFERENCES main (nickname) );", nameOfBlacklist);
        //добавляем имя таблицы черного списка в строку пользователя
        String sql2 = String.format("UPDATE main SET name_blacklists = '%s' WHERE nickname = '%s'", nameOfBlacklist, nickOfOwner);

        try {
            // оправка запроса и получение ответа из БД
            int rs = stmt.executeUpdate(sql1 + sql2);

            // если таблица создана, то возвращается 1, если нет, то вернеться 0?
            if(rs != 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Метод добавления имени в таблицу черного списка пользователя в БД
    //если строка добавлена, то возвращается true
    public static boolean addNicknameIntoBlacklistInDB(String nickOfOwner, String nickname){
        //находим имя таблицы черного списка по имени его владельца
        String nameOfBlacklistTable = getUserBlacklistNameByNicknameInDB(nickOfOwner);
        //добавляем в таблицу черного списка имя пользователя
        // формирование запроса. '%s' - для последовательного подставления значений в соотвествующее место
        String sql = String.format("INSERT INTO %s (nickname) VALUES ('%s')", nameOfBlacklistTable, nickname);

        try {
            // оправка запроса и получение ответа из БД
            int rs = stmt.executeUpdate(sql);

            // если строка добавлена, то возвращается 1, если нет, то вернеться 0?
            if(rs != 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Метод проверки имени в таблице черного списка пользователя в БД
    //если такое имя есть, то возвращается true
    public static boolean checkUserInBlacklistDB(String nickOfOwner, String nickname){
        //находим имя таблицы черного списка по имени его владельца
        String nameOfBlacklistTable = getUserBlacklistNameByNicknameInDB(nickOfOwner);
        //проверяем есть вообще черный список
        if(nameOfBlacklistTable == null){
            //возвращаем false, если нет таблицы
            return false;
        }
        //ищем имя пользователя в таблице черного списка
        // формирование запроса. '%s' - для последовательного подставления значений в соотвествующее место
        String sql = String.format("SELECT nickname FROM %s WHERE nickname = '%s'", nameOfBlacklistTable, nickname);

        try {
            // оправка запроса и получение ответа из БД
            ResultSet rs = stmt.executeQuery(sql);

            // если есть строка возвращаем результат, если нет, то вернеться null
            if(rs.next()) {
                return (rs.getString(1)).equals(nickname);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Метод удаления имени из таблицы черного списка пользователя в БД
    //если строка удалена, то возвращается true
    public static boolean deleteUserFromBlacklistDB(String nickOfOwner, String nickname){
        //находим имя таблицы черного списка по имени его владельца
        String nameOfBlacklistTable = getUserBlacklistNameByNicknameInDB(nickOfOwner);

        // формирование запроса. '%s' - для последовательного подставления значений в соотвествующее место
        //удаляем имя пользователя из таблицы черного списка
        String sql = String.format("DELETE FROM %s WHERE nickname = '%s'", nameOfBlacklistTable, nickname);
        try {
            // оправка запроса и получение ответа из БД
            int rs = stmt.executeUpdate(sql);

            // если строка удалена, то возвращается 1, если нет, то вернеться 0?
            if(rs != 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Метод отключения от БД
     */
    public static void disconnect() {
        try {
            // закрываем соединение
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
