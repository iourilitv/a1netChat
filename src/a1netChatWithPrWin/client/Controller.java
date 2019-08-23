package a1netChatWithPrWin.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static javafx.scene.text.TextAlignment.RIGHT;

public class Controller {

    @FXML
    HBox mainChatPanel;

    @FXML
    ScrollPane scrollPaneChat;

    @FXML
    VBox vBoxChat;

    @FXML
    HBox bottomPanel;

    @FXML
    TextField textField;

    @FXML
    Button btn1;
    //ListView - динамическое представление для работы с GUI framework JavaFX
    @FXML
    ListView<String> clientList;

    @FXML
    VBox registrationForm;

    @FXML
    VBox regFormTopLabelsBox;

    @FXML
    HBox regFormNicknameBox;

    @FXML
    TextField regFormNickField;

    @FXML
    TextField regFormLoginField;

    @FXML
    PasswordField regFormPasswordField;

    @FXML
    TextArea regFormTextArea;

    @FXML
    HBox regFormRegBtnsBox;

    @FXML
    Button regFormSendToRegisterBtn;

    @FXML
    Button regFormCancelBtn;

    @FXML
    HBox regFormAuthBtnsBox;

    @FXML
    Button regFormAuthBtn;

    @FXML
    Button regFormRegisterBtn;

    //переменные с pr в начале имени - для privateChat.fxml
    @FXML
    VBox prVBoxChat;

    @FXML
    Label prMsgLabel;

    @FXML
    TextField prTextField;

    @FXML
    Button prBtnSend;

    @FXML
    HBox prBottomPanel;

    //константы для подгонки размеров изображения
    public static final double SCROLLBAR_WIDTH = 20;
    public static final double SCROLLPANE_HEIGHT_SHIFT = 35;
    public static final double LABEL_PROPORTION = 0.8;
    public static final double CHARACTER_IN_PIXELS = 7;

    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    private boolean isAuthorized;

    //static - чтобы использовать в потоке FX application
    private static String chatCompanionNick;

    //TODO pr.window opening.Deleted
    //чтобы закрыть окно приватного чата в методе connect
    //private static PrivateChatStage prStage;
    //TODO pr.window opening.Added
    //чтобы закрыть окно приватного чата в методе
    //static - чтобы использовать в потоке FX application
    private static PrivateMsgWindow prMsgWindow;

    private String nick;

    final String IP_ADRESS = "localhost";//IP 127.0.0.1.
    final int PORT = 8189;

    public Controller() {
    }

    //метод отображения элементов GUI в режиме Авторизован/Неавторизован
    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;

        //очищаем поле от сообщений
        regFormTextArea.clear();

        if (!isAuthorized) {
            registrationForm.setVisible(true);//панель авторизации и регистрации
            registrationForm.setManaged(true);
            mainChatPanel.setVisible(false);
            mainChatPanel.setManaged(false);

        } else {
            registrationForm.setVisible(false);//делаем окно видимым (по умолчанию в sample visible="false")
            registrationForm.setManaged(false);//выделяется место под HBox, если окно видимо (по умолчанию в sample managed="false")
            mainChatPanel.setVisible(true);
            mainChatPanel.setManaged(true);
        }
    }

    //метод отображения элементов GUI в режиме Зарегистрирован/Незарегистрирован
    public void setRegistered(boolean isRegistered){

        //на всякий случай скрываем панель основного чата
        mainChatPanel.setVisible(false);
        mainChatPanel.setManaged(false);

        if(!isRegistered){
            //если не зарегистрирован, то
            //открываем блок верхних меток, пояснений для регистрации
            regFormTopLabelsBox.setVisible(true);
            regFormTopLabelsBox.setManaged(true);
            //открываем блок Имени
            regFormNicknameBox.setVisible(true);
            regFormNicknameBox.setManaged(true);
            //открываем блок кнопок для регистрации
            regFormRegBtnsBox.setVisible(true);
            regFormRegBtnsBox.setManaged(true);
            //скрываем блок кнопок авторизации
            regFormAuthBtnsBox.setVisible(false);
            regFormAuthBtnsBox.setManaged(false);

        } else{
            //если зарегистрирован, то наоборот
            //скрываем блок верхних меток, пояснений для регистрации
            regFormTopLabelsBox.setVisible(false);
            regFormTopLabelsBox.setManaged(false);
            //скрываем блок Имени
            regFormNicknameBox.setVisible(false);
            regFormNicknameBox.setManaged(false);
            //скрываем блок кнопок для регистрации
            regFormRegBtnsBox.setVisible(false);
            regFormRegBtnsBox.setManaged(false);
            //открываем блок кнопок авторизации
            regFormAuthBtnsBox.setVisible(true);
            regFormAuthBtnsBox.setManaged(true);
        }
    }

    /**
     * Метод подсоединения аналогичный серверному
     */
    public void connect() {
        try {
            socket = new Socket(IP_ADRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean serverClosed = false;

                        //***Блок для авторизации и регистрации***
                        while (true) {
                            String str = in.readUTF();

                            //TODO Временно
                            System.out.println("блок для авторизации и регистрации. while str: " + str);

                            //ловим все служебные сообщения, чтобы не выводить их в TextArea
                            if (str.startsWith("/")) {

                                //ловим сообщение от сервера об отключении
                                if (str.equals("/serverclosed")) {
                                    //устанавливаем флаг, что сервер отключен, чтобы не перейти в блок отправки сообщений
                                    serverClosed = true;
                                    break;
                                }

                                //если пришло подтверждение авторизации, переходим в форму чата и прерываем процесс
                                if (str.startsWith("/authok")) {
                                    //скрываем элементы GUI для регистрации
                                    setRegistered(true);
                                    //скрываем элементы GUI для авторизации
                                    setAuthorized(true);
                                    //выделяем полученный с сервера собственный ник пользователя
                                    int splitLimit = 2;
                                    String[] tokens = str.split(" ", splitLimit);
                                    nick = tokens[1];
                                    break;
                                }

                                //если пришло подтверждение регистрации, отправляем запрос на авторизацию, не прерывая процесс//TODO Не работает
                                if (str.startsWith("/regok")) {
                                    //скрываем элементы GUI для регистрации
                                    setRegistered(true);
                                    //открываем элементы GUI для авторизации
                                    setAuthorized(false);

                                    //разделяем сообщение на три части
                                    //TODO Не работает авторизация после регистрации.Удалил.Не достаточно
                                    //int splitLimit = 3;
                                    //String[] tokens = str.split(" ", splitLimit);
                                    //отправляем сообщение с логином и паролем для регистрации
                                    //out.writeUTF("/auth " + tokens[1] + tokens[2]);
                                }
                            } else {
                                //выводим сообщения в панель регистрационной формы
                                regFormTextArea.appendText(str + "\n");
                            }
                        }

                        //Устанавливаем пустой ник партнера для приватного чата пользователя
                        chatCompanionNick = null;

                        //***Блок для разбора сообщений***
                        //проверяем флаг, что сервер отключен, чтобы не начать отслеживать сообщения после отключения сервера
                        while (!serverClosed) {
                            String str = in.readUTF();

                            //ловим все служебные сообщения, чтобы не выводить их в TextArea
                            if (str.startsWith("/")) {
                                if (str.equals("/serverclosed")) {
                                    break;
                                }

                                //обрабатываем запрос от сервера на добавление клиента в список
                                if (str.startsWith("/clientlist")) {
                                    String[] tokens = str.split(" ");
                                    //всегда, когда меняем графическую часть интерфейса, нужно
                                    //всю работу осуществлять в отдельном потоке для работы с интеграцией с GUI.
                                    // Это особенность JavaFX, возможно это уже реализовано в TreeView? Но лучше вставлять всегда
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            //обновляем список пользователей
                                            clientList.getItems().clear();
                                            for (int i = 1; i < tokens.length; i++) {
                                                clientList.getItems().add(tokens[i]);
                                            }
                                        }
                                    });
                                }

                                //TODO pr.window opening.Deleted
                                //***Обработка запросов на инициализацию и закрытие персонального чата***
                                /*if (str.startsWith("/inv")) {//всегда в строке ник партнера
                                    // ***Обработка запроса на закрытие персонального чата***
                                    if (str.startsWith("/invend")) {
                                        //выводим пользователю сервисное сообщение в окно основного чата.
                                        showMessage(vBoxChat, Pos.TOP_LEFT, "service: Your partner " + chatCompanionNick + " has left. Your private chat window has been closed.");
                                        //Обнуляем ник партнера по чату
                                        chatCompanionNick = null;
                                        //закрываем свое окно приватного чата, если партнер его закрыл
                                        Platform.runLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                prStage.close();
                                            }
                                        });
                                    }
                                    //передаем сообщения на проверку в метод инициализации прив.чата
                                    initPrivateChat(str);
                                }*/

                                //TODO onlyPrivateMsgWindow.Переписать?
                                //обрабатываем полученные сообщения из приватного чата
                                if (str.startsWith("/w")) {

                                    //TODO временно
                                    System.out.println("connect.if (str.startsWith(\"/w\")).str: " + str);

                                    int splitLimit = 3;
                                    String[] temp = str.split(" ", splitLimit);
                                    //выделяем ник и собственно текст сообщения
                                    String nick = temp[1];
                                    String msg = temp[2];
                                    //открывает окно дла приема и отправки одного приватного сообщения
                                    openPrivateMessageWindow(nick, msg);
                                }
                            } else{
                                //выводим сообщение в свое окно чата
                                showMessage(vBoxChat, Pos.TOP_LEFT, str);
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Server connection is lost: " + e);
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //скрываем элементы GUI для регистрации
                        setRegistered(true);
                        //открываем элементы GUI для авторизации
                        setAuthorized(false);
                        //выводим сообщение пользователю
                        regFormTextArea.appendText("Waiting for server connection...Please log in.\n");
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            System.out.println("Waiting for server connection...: " + e);
        }
    }

    //метод запроса на регистрацию по нажатию элемента регистрация в форме авторизации(upperPanel)
    @FXML
    public void tryToRegister(){
        //открываем окно регистрационной формы
        setRegistered(false);
        regFormTextArea.clear();
    }

    //метод запроса на регистрацию(сохранение) данных из регистрационной формы по событию кнопки Отправить
    @FXML
    public void getRegistration(){
        if (socket == null || socket.isClosed()) {
            // сначала подключаемся к серверу, если не подключен(сокет не создан или закрыт)
            //если сервер еще не запущен, выводим сообщение и пытаемся подключиться в бесконечном цикле
            while(socket == null || socket.isClosed()){
                connect();
            }
        }
        try {
            //TODO удалить? Лучше очищать перед выводом текста.
            //очищаем поле от старых сообщений
            regFormTextArea.clear();
            // отправка на сервер запроса на регистрацию данных введенных в форме регистрации
            out.writeUTF("/reg " + regFormNickField.getText() + " " + regFormLoginField.getText() + " " + regFormPasswordField.getText());

            //TODO Временно
            System.out.println("getRegistration() str:" + "/reg " + regFormNickField.getText() + " " + regFormLoginField.getText() + " " + regFormPasswordField.getText());

            //Не нужно очищать поле, если не зарегистрировался, чтобы не вбивать еще раз.//TODO Проверить надо ли?
            regFormNickField.clear();//очищаем поле имени в чате
            regFormLoginField.clear();//очищаем поле логина
            regFormPasswordField.clear();//очищаем поле пароля
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Метод по нажатию кнопки в форме регистрации
    @FXML
    public void cancelRegistration(){
        //возвращаемся из регистрационной формы в авторизационную
        setRegistered(true);
        regFormLoginField.clear();
        regFormPasswordField.clear();
        regFormTextArea.clear();
    }

    //Метод для авторизации. Запускается кнопкой Авторизоваться в форме регистрации(upperPanel)
    @FXML
    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            // сначала подключаемся к серверу, если не подключен(сокет не создан или закрыт)
            //если сервер еще не запущен, выводим сообщение и пытаемся подключиться в бесконечном цикле
            while(socket == null || socket.isClosed()){
                connect();
            }
        }
        try {
            //TODO удалить? Лучше очищать перед выводом текста.
            //очищаем поле от старых сообщений
            regFormTextArea.clear();
            // отправка сообщений на сервер для авторизации
            out.writeUTF("/auth " + regFormLoginField.getText() + " " + regFormPasswordField.getText());
            regFormLoginField.clear();//очищаем поле логина
            regFormPasswordField.clear();//очищаем поле пароля
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO pr.window opening.Deleted
    /*//метод для приглашения в приватный чат выбранного в списке ник
    @FXML
    public void tryToInviteIntoPrivateChat(MouseEvent mouseEvent) throws IOException {
        //проверяем сколько было кликов мышью
        if (mouseEvent.getClickCount() == 2) {

            //TODO Временно
            System.out.println("Двойной клик");

            //запрещаем кликать на свой ник в списке
            if (clientList.getSelectionModel().getSelectedItem().equals(nick)){
                //Выводим предупреждение пользователю в GUI
                showMessage(vBoxChat, Pos.TOP_LEFT, "service: Нельзя выбрать самого себя!");
                //Обнуляем ник партнера по чату
                chatCompanionNick = null;

                //TODO временно
                System.out.println("Нельзя выбрать самого себя!");

                return;
            }

            //проверяем не начат ли уже с кем-то приватный чат
            //другой не сможет пригласить, если пользователь уже чатится с кем-то приватно
            if (chatCompanionNick == null) {
                //резервируем прив.чат для партнера
                chatCompanionNick = clientList.getSelectionModel().getSelectedItem();
                //отправляем партнеру приглашение початиться
                try {
                    // отправляем сообщение приглашение партнеру начать приватный чат
                    out.writeUTF("/invto " + chatCompanionNick);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else{
                //Выводим предупреждение пользователю в GUI
                showMessage(vBoxChat, Pos.TOP_LEFT, "service: Ваш приватный чат занят!");

                //TODO временно
                System.out.println("Ваш приватный чат занят!");

            }
        }
    }*/
    //TODO pr.window opening.Added
    //метод открытия окна отправки приватного сообщения
    @FXML
    private void tryToOpenPrivateMessageWindow(MouseEvent mouseEvent) throws IOException {
        String nickTo;//кому отправляем приватное сообщение

        //проверяем сколько было кликов мышью. Нужен двойной клик
        if (mouseEvent.getClickCount() == 2) {

            //TODO Временно
            System.out.println("Двойной клик");

            nickTo = clientList.getSelectionModel().getSelectedItem();

            //запрещаем кликать на свой ник(nick) в списке
            if (nickTo.equals(nick)){
                //Выводим предупреждение пользователю в GUI
                showMessage(vBoxChat, Pos.TOP_LEFT, "service: Нельзя выбрать самого себя!");

                //TODO временно
                System.out.println("Нельзя выбрать самого себя!");

            } else {
                //открываем окно для ввода сообщения
                openPrivateMessageWindow(nickTo, null);
            }
        }
    }

    //TODO pr.window opening.Deleted
    //метод инициализации окна приватного чата
    /*private void initPrivateChat(String str) {
        try {
            //выделяем ник партнера по чату из служебного сообщения
            String[] temp = str.split(" ", 2);

            //если приглашение початиться пришло от партнера
            if (str.startsWith("/invto")) {
                //проверяем свободен ли мой приватный чат
                if (chatCompanionNick == null) {
                    //если мой чат тоже свободен, резервируем его для ника партнера
                    chatCompanionNick = temp[1];
                    //инициализируем приватный чат
                    startPrivateChat();
                    //отправляем на мой сервер подтверждение
                    out.writeUTF("/invok " + chatCompanionNick);
                } else {
                    //отправляем на мой сервер отказ чатиться
                    out.writeUTF("/invnot " + chatCompanionNick);
                }
                return;
            }

            //если от партнера пришел отказ(его чат занят)
            if (str.startsWith("/invnot")) {
                //Выводим служебное сообщение пользователю в GUI
                showMessage(vBoxChat, Pos.TOP_LEFT, "service: Адресат занят другим приватным чатом!");

                //TODO Временно.
                System.out.println("Адресат занят другим приватным чатом!");

                return;
            }

            //если от партнера пришло подтверждение, что все готово начать чат
            if (str.startsWith("/invok")) {
                //инициализируем приватный чат
                startPrivateChat();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    //TODO pr.window opening.Deleted
    //если приватный чат инициализирован, открыть новое окно
    //и подтверждаем готовность инициализации приватного чата
    /*private void startPrivateChat(){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    //открываем отдельное окно для приватного чата
                    prStage = new PrivateChatStage(Controller.this, prVBoxChat);
                    prStage.show();

                    //обработчик закрытия окна персонального чата
                    prStage.setOnCloseRequest(event -> {
                        //отправляем запрос партнеру по чату о закрытии окна чата
                        //если окно закрывается после сообщения от партнера о выходе из приватного чата,
                        // а не по крестику закрыть окно
                        if(chatCompanionNick != null){
                            try {
                                out.writeUTF("/invend " + chatCompanionNick);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //Обнуляем ник партнера по чату
                            chatCompanionNick = null;
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }*/
    //TODO pr.window opening.Added
    //Метод открывает окно для приема и отправки одного приватного сообщения
    private void openPrivateMessageWindow(String nick, String msg){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    //открываем отдельное окно для приватного чата
                    prMsgWindow = new PrivateMsgWindow(Controller.this, nick, msg);

                    //всегда показываем окно выше остальных окон
                    prMsgWindow.isAlwaysOnTop();
                    prMsgWindow.show();

                    //называем окно в зависимости от направления
                    //TODO ERR.java.lang.NullPointerException
                    /*if(msg != null){//для отправки сообщения
                        prMsgLabel.setText(msg);
                    } else {//при получении сообщения
                        prMsgLabel.setText("Введите сообщение и нажмите Send или Enter");

                    }*/

                    //обработчик закрытия окна персонального чата
                    prMsgWindow.setOnCloseRequest(event -> {
                        //закрываем по крестику окно приватного сообщения
                        prMsgWindow.close();
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //Метод отправки запроса об отключении на сервер
    public void dispose() {
        System.out.println("Отправляем сообщение о закрытии");
        try {
            //проверяем подключен ли клиент
            if (out != null && !socket.isClosed()) {
                out.writeUTF("/end");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //метод для отправки сообщений в общем чате с выводом пользователю
    @FXML
    public void sendMsg(ActionEvent actionEvent) {
        try {
            //принимаем строку из текстового поля
            String str = textField.getText();

            //TODO Don't allow sending of empty string or with only break spaces.Добавил
            //не отправляем пустую строку, в том числе из одних пробелов
            if(str.equals("") || str.split(" ").length < 1) {//TODO Важно! если str = "", то str.split(" ").length = 1!!!
                textField.clear();
                textField.requestFocus();
                return;
            }

            //не показываем служебные сообщения у себя
            if(!str.startsWith("/")) {
                //выводим пользователю собственное сообщение в окно основного чата.
                showMessage(vBoxChat, Pos.TOP_RIGHT, str);
            }
            //TODO L8hwTask5.Add msg to prChat.Добавил
            //отправляем сообщение на сервер(ClientHandler)
            out.writeUTF(str);
            //очищаем текстовое поле и возвращаем ему курсор
            textField.clear();
            textField.requestFocus();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //TODO pr.window opening.Deleted
    //метод для отправки сообщений в приватном чате
    /*@FXML
    public void sendMsgInPrivateChat (ActionEvent actionEvent) {
        try {
            //принимаем строку из текстового поля
            String str = prTextField.getText();

            //не отправляем пустую строку, в том числе из одних пробелов
            if(str.equals("") || str.split(" ").length < 1) {
                prTextField.clear();
                prTextField.requestFocus();
                return;
            }

            //не показываем служебные сообщения у себя
            if(!str.startsWith("/")) {
                //выводим пользователю собственное сообщение в окно приватного чата.
                showMessage(prVBoxChat, Pos.TOP_RIGHT, str);
            }
            //отправляем сообщение на сервер(ClientHandler)
            DataOutputStream out = ((PrivateChatStage)prBtnSend.getScene().getWindow()).out;
            out.writeUTF("/w " + chatCompanionNick + " " + str);
            //очищаем текстовое поле и возвращаем ему курсор
            prTextField.clear();
            prTextField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
    //TODO pr.window opening.Added
    //метод для отправки сообщений в приватном чате
    @FXML
    public void sendPrivateMsg (ActionEvent actionEvent) {
        try {
            //принимаем строку из текстового поля
            String str = prTextField.getText();
            //получаем ник получателя
            String nickTo = ((PrivateMsgWindow)prBtnSend.getScene().getWindow()).nick;
            //не отправляем пустую строку, в том числе из одних пробелов
            if(str.equals("") || str.split(" ").length < 1) {
                prTextField.clear();
                prTextField.requestFocus();
                return;
            }
            //отправляем сообщение на сервер(ClientHandler)
            DataOutputStream out = ((PrivateMsgWindow)prBtnSend.getScene().getWindow()).out;
            out.writeUTF("/w " + nickTo + " " + str);

            //TODO временно
            System.out.println("sendPrivateMsg.out.writeUTF(\"/w \" + nickTo + \" \" + str): " + "/w " + nickTo + " " + str);

            //закрываем после отправки окно приватного сообщения
            prMsgWindow.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Метод вывода полученных и сервисных сообщений в чаты пользователя
    private void showMessage(VBox vBoxCh, Pos position, String msg){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                //создаем метку сообщения
                Label label = new Label(msg);

                //устанавливаем это сообщение собственное или получено
                if(position.equals(Pos.TOP_RIGHT)){//для своих сообщений
                    //устанавливаем цвета фона и текста в метке и скругление метки
                    label.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white; -fx-background-radius: 5");

                    //TODO Важно! Выравнивание текта в метке Не полностью работает!
                    // работает только для нескольких строк в метке
                    //выравниваем текст по правому краю метки
                    label.setTextAlignment(RIGHT);//неработает и (TextAlignment.RIGHT)

                } else {//для полученных сообщений
                    //устанавливаем цвета фона и текста в метке и скругление метки
                    label.setStyle("-fx-background-color: bisque; -fx-text-fill: black; -fx-background-radius: 5");
                }

                //перенос слов в метке
                //вычисляем в пикселях длину первой линии текста (количество символов * на 7 пикселей)
                double textLineLength = (label.getText().length() + 2) * CHARACTER_IN_PIXELS;
                //вычисляем в пикселях ширину главного бокса боксов меток и вычитаем 20%, чтобы метка была немного меньше
                double scrollBarWidthShift = 2;
                //определяем показывается ли панель прокрутки
                if(scrollPaneChat.getVvalue() >= scrollPaneChat.getHeight() - SCROLLPANE_HEIGHT_SHIFT){
                    //если да, устанавливаем размер сдвига на ширину панели прокрутки
                    scrollBarWidthShift += SCROLLBAR_WIDTH;
                }
                double vBoxChLength = scrollPaneChat.getWidth() - scrollBarWidthShift;
                //устанавливаем ширину vBoxCh для адаптивной подгонки при расширении окна чата
                vBoxCh.setMinWidth(vBoxChLength);
                vBoxCh.setPrefWidth(vBoxChLength);
                vBoxCh.setMaxWidth(vBoxChLength);

                //если длина первой линии меньше длины от панели прокрутки, то устанавливаем ширину метки по длине линии текста
                if(textLineLength < vBoxChLength){
                    label.setMinWidth(textLineLength);
                    label.setPrefWidth(textLineLength);
                    label.setMaxWidth(textLineLength);
                } else{//если нет - по панели прокрутки с уменьшающим коэффициентом
                    label.setMinWidth(vBoxChLength * LABEL_PROPORTION);
                    label.setPrefWidth(vBoxChLength * LABEL_PROPORTION);
                    label.setMaxWidth(vBoxChLength * LABEL_PROPORTION);
                }

                //устанавливаем перенос слов в метке сообщения
                label.setWrapText(true);
                //устанавливаем отступ текста от края метки
                label.setPadding(new Insets(5));//-fx-padding: 5;
                //создаем бокс с меткой сообщения
                VBox vBox = new VBox();
                //устанавливаем отступ по всем направлениям, привязанный к элементу
                vBox.setMargin(label, new Insets(5));//-fx-padding:5; -fx-spacing:5;
                //устанавливаем позицию метки в боксе
                vBox.setAlignment(position);
                //добавляем метку в бокс
                vBox.getChildren().add(label);
                //добавляем vBox в общий бокс чата
                vBoxCh.getChildren().add(vBox);
                //Устанавливаем положение прокрутки вниз(показываем последний элемент)
                //привязываем свойство vvalue к свойству высоты внутреннего вертикального бокса чата
                scrollPaneChat.vvalueProperty().bind(vBoxCh.heightProperty());
            }
        });
    }

    public VBox getPrVBoxChat() {
        return prVBoxChat;
    }

    public TextField getPrTextField() {
        return prTextField;
    }

    public Button getPrBtnSend() {
        return prBtnSend;
    }

    public HBox getPrBottomPanel() {
        return prBottomPanel;
    }

    public Socket getSocket() {
        return socket;
    }

    public DataInputStream getIn() {
        return in;
    }

    public DataOutputStream getOut() {
        return out;
    }

    public String getChatCompanionNick() {
        return chatCompanionNick;
    }
}
