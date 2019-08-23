package a1netChatWithPrWin.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private MainServer server;
    private String nick;

    public ClientHandler(Socket socket, MainServer server) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            //анонимный класс
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean clientWindowClosed = false;

                        //***Цикл для авторизации и регистрации***
                        // Крутится бесконечно, пока пользователь не авторизуется
                        while (true) {
                            String str = in.readUTF();
                            if (str.equals("/end")) {
                                //устанавливаем флаг, что сервер отключен, чтобы не перейти в блок отправки сообщений
                                clientWindowClosed = true;
                                //закрываем клиента после удаления его из списка
                                out.writeUTF("/serverclosed");
                                break;
                            }

                            //если получено сообщение связанное с регистрацией
                            if(str.startsWith("/reg")) {
                                int splitLimit = 4;
                                //String[] tokens = str.split(" ", splitLimit);//TODO ERR.Пустые поля
                                String[] tokens = str.split(" ");

                                //проверяем есть ли в форме пустые поля
                                if(tokens.length >= splitLimit) {

                                    String login = tokens[2];
                                    String password = tokens[3];
                                    String nickname = tokens[1];

                                    //делаем запрос в БП, есть ли такой логин или ник
                                    if (AuthService.checkLoginAndNicknameInDB(tokens)) {//нет такого в базе

                                        //записываем в БД данные из формы
                                        if (AuthService.addUserIntoDB(tokens)) {

                                            //отправляем сообщение c логином и паролем для прохождения авторизации без повторного ввода
                                            sendMsg("/regok " + login + " " + password);
                                            //выводим сообщение пользователю об успешной регистрации
                                            sendMsg("Вы зарегистрированы. Нажмите Авторизоваться, чтобы войти.");

                                            //выводим сообщение в консоль сервера об успешной регистрации клиента
                                            System.out.println("Пользователь " + nickname + " успешно зарегистрирован.");
                                        } else {
                                            //нет, если этот логин уже занят
                                            sendMsg("Неудачная попытка регистрации!\nПопробуйте еще раз.");
                                        }
                                    } else {
                                        //нет, если этот логин уже занят
                                        sendMsg("Пользователь с таким логином или именем уже зарегистрирован!\n Введите другие логин и имя.");
                                    }
                                } else {
                                    //есть пустые поля
                                    sendMsg("Нельзя отправлять форму с пустыми полями!");
                                }
                            }

                            //если сообщение начинается с /auth
                            if(str.startsWith("/auth")) {

                                //чтобы избежать ошибки при пустом вводе в поля login или пароль
                                int splitLimit = 3;
                                String[] tokens = str.split(" ");

                                //проверяем есть ли в форме пустые поля
                                if(tokens.length >= splitLimit) {

                                    String login = tokens[1];
                                    String password = tokens[2];

                                    // Вытаскиваем данные из БД //здесь: tokens[1] - логин, tokens[2] - пароль
                                    String newNick = AuthService.getNickByLoginAndPass(login, password);
                                    if (newNick != null) {
                                        //проверяем не авторизовался ли кто-то уже под этим ником
                                        if(!server.isThisNickAuthorized(newNick)){
                                            nick = newNick;
                                            //отправляем сообщение(в Controller) об успешной авторизации с собственным ником пользователя
                                            sendMsg("/authok " + nick);
                                            //подписываем клиента при успешной авторизации и выходим из цикла
                                            server.subscribe(ClientHandler.this);
                                            //выводим сообщение в консоль сервера об успешном подключении клиента
                                            System.out.println("Клиент с ником " + nick + " подключился.");
                                            break;
                                        }
                                        else{
                                            sendMsg("Пользователь " + newNick + " уже авторизован!");
                                        }
                                    } else {
                                        sendMsg("Вы ввели неверный логин/пароль или не зарегистрированы!\nДля регистрации нажмите \"Регистрация\"");
                                    }
                                } else {
                                    //есть пустые поля
                                    sendMsg("Нельзя отправлять форму с пустыми полями!");
                                }
                            }
                        }//while

                        //***Блок для отправки сообщений***
                        //проверяем флаг, что сервер отключен, чтобы не начать отслеживать сообщения после закрытия окна клиента
                        while (!clientWindowClosed) {
                            String str = in.readUTF();
                            //отлавливаем все служебные сообщения
                            if (str.startsWith("/")){
                                //запрос на отключение
                                if (str.equals("/end")) {
                                    //закрываем клиента после удаления его из списка
                                    out.writeUTF("/serverclosed");
                                    break;
                                }

                                //оправка персонального сообщения
                                if(str.startsWith("/w")) {
                                    //ClientHandler.this вместо nick, чтобы отправить предупреждение отправителю,
                                    //что нельзя отправлять самому себе

                                    //TODO временно
                                    System.out.println("ClientHandler.if(str.startsWith(\"/w\")) {. str: " + str);

                                    //TODO когда добавится адресная книга, этот блок не понадобится
                                    String nickOfRecipient;//ник адресата
                                    String msg;//текст сообщения адресату
                                    //разделяем по пробелу на splitLimit ячеек массива,
                                    //чтобы избежать ошибки при неполном вводе сервисного сообщения
                                    //limit = splitLimit - количество возвращаемых строк.
                                    int splitLimit = 3;
                                    String[] temp = str.split(" ", splitLimit);
                                    //проверка корректности синтаксиса сервисного сообщения
                                    if(temp.length >= splitLimit){
                                        //выделяем ник адресата
                                        nickOfRecipient = temp[1];
                                        //выделяем собственно текст сообщения
                                        msg = temp[2];
                                        //проверка не отправляется ли сообщение самому себе
                                        if(!ClientHandler.this.getNick().equals(nickOfRecipient)){//TODO лишняя проверка?
                                            //проверяем не находится ли получатель черном списке отправителя
                                            if(!AuthService.checkUserInBlacklistDB(ClientHandler.this.getNick(), nickOfRecipient)){
                                                //отправляем сообщение адресату
                                                server.sendMsgToNick(ClientHandler.this, nickOfRecipient, msg);

                                            } else{
                                                //если получатель находится в черном списке адресата (цикл не прервался по return)
                                                //отправляем сообщение отправителю(себе)
                                                ClientHandler.this.sendMsg("Адресат с ником " + nickOfRecipient + " в вашем черном списке!");
                                            }
                                        } else{
                                            //отправка предупреждения отправителю
                                            ClientHandler.this.sendMsg("Нельзя отправлять самому себе!");
                                        }
                                    } else{
                                        //отправка предупреждения отправителю
                                        ClientHandler.this.sendMsg("Неверный синтаксис сервисного сообщения!");
                                    }
                                }

                                //отлавливаем сообщения о черном списке
                                if (str.startsWith("/blist")) {
                                    String[] tokens = str.split(" ", 2);
                                    //получаем имя, кого заносим в черный список
                                    String nickname = tokens[1];
                                    //получаем имя, владельца черного списка
                                    String nickOfOwner = ClientHandler.this.getNick();

                                    //отлавливаем сообщение о добавлении в черный список
                                    if (str.startsWith("/blistadd")) {
                                        //сначала проверяем нет ли уже такого имени в черном списке (true - есть)
                                        //если нет ни имени в таблице, ни даже таблицы, вернется false
                                        if (!AuthService.checkUserInBlacklistDB(nickOfOwner, nickname)) {
                                            //проверяем создана ли таблица черного списка (null - нет)
                                            if (AuthService.getUserBlacklistNameByNicknameInDB(nickOfOwner) == null) {
                                                //создаем таблицу черного списка, если таблицы нет
                                                System.out.println(AuthService.createUserBlacklistInDB(nickOfOwner));
                                            }
                                            //добавляем имя пользователя в таблицу черного списка
                                            System.out.println(AuthService.addNicknameIntoBlacklistInDB(nickOfOwner, nickname));
                                            //выводим сообщение владельцу
                                            sendMsg("Вы добавили пользователя " + nickname + " в ваш черный список");
                                        } else {
                                            sendMsg("Пользователь с таким именем уже есть в вашем черном списке!");
                                        }
                                    }

                                    //отлавливаем сообщение об удалении из черного списка
                                    if (str.startsWith("/blistdel")) {
                                        //сначала проверяем нет ли уже такого имени в черном списке (true - есть)
                                        //если нет ни имени в таблице, ни даже таблицы, вернется false
                                        if (AuthService.checkUserInBlacklistDB(nickOfOwner, nickname)) {
                                            //если имя есть, то и таблица черного списка тоже есть
                                            //удаляем имя из черного списка
                                            AuthService.deleteUserFromBlacklistDB(nickOfOwner, nickname);
                                            //выводим сообщение владельцу
                                            sendMsg("Вы удалили пользователя " + nickname + " из вашего черного списка");
                                        } else {
                                            sendMsg("Пользователя с таким именем нет в вашем черном списке!");
                                        }
                                    }
                                }
                            }//if "/"
                            else{
                                server.broadcastMsg(ClientHandler.this,nick + ": " + str);
                            }
                        }//while
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    //Важно. this здесь - это объект анонимного класса Thread и обратиться к нему
                    // можно только через основной класс ClientHandler
                    server.unsubscribe(ClientHandler.this);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //геттер для nick
    public String getNick() {
        return nick;
    }

    //метод отправки сообщения своему пользователю(в Controller)
    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
