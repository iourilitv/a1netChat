package a1netChatWithPrWin.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Java Core.
 * Net chat with the a window for receiving and sending only single private message.
 * Developing started on 16.08.2019.
 * @author Yuriy Litvinenko.
 */
public class MainClient extends Application {

    //создаем экземпляр контроллера
    Controller contr;

    @Override
    public void start(Stage primaryStage) throws Exception{
        //чтобы получить доступ к контроллеру
        //лоадер вынесли отдельно, чтобы с ним удобнее было работать
        FXMLLoader loader = new FXMLLoader();
        //с помощью метода getResourceAsStream извлекаем данные из лоадера, чтобы
        //вызвать метод getController для получения контроллера
        Parent root = loader.load(getClass().getResourceAsStream("mainChatStage.fxml"));
        contr = loader.getController();

        primaryStage.setTitle("a1netChatWithPrWin 2k19");
        Scene scene = new Scene(root, 350, 350);
        primaryStage.setScene(scene);
        primaryStage.show();

        //определяем действия по событию закрыть окно по крестику через лямбда
        //лямбда здесь - это замена анонимного класса типа new Runnable
        //в лямбда event - аргумент(здесь некое событие), {тело лямбды - операции}
        primaryStage.setOnCloseRequest(event -> {
            contr.dispose();//dispose - располагать, размещать
            //сворачиваем окно
            Platform.exit();
            //указываем системе, что выход без ошибки
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
