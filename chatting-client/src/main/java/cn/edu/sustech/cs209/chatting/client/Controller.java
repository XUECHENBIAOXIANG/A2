package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {
    private List<String> users = new ArrayList<>();
    List<String> usernames;
    @FXML
    ListView<String> onlineUsersList;
    @FXML
    ListView<Message> chatContentList;
    @FXML
    TextArea inputArea;
    InputStream inputStream;
    OutputStream outputStream;
    Scanner in;
    PrintWriter out;
    String username="";
    Socket socket;
    String denglv="";
    Long chating= Long.valueOf(1);
    ObservableList<Message> messages = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {

            socket=new Socket("localhost",8888);
            inputStream=socket.getInputStream();
            outputStream=socket.getOutputStream();
            in=new Scanner(inputStream);
            out=new PrintWriter(outputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            while (!Thread.interrupted()) {  // 修改while条件，当线程被中断时跳出循环
                if (in.hasNext()){
                    String line=in.nextLine();
                    Message message= Message.fromJson(line);

                    switch (message.getType()){
                        case  CONNECT:
                            username=message.getSendTo();

                            break;
                        case DISCONNECT:
                            denglv="shibai";
                            Platform.runLater(() -> {
                                // 在 UI 线程中弹出提示
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Error");
                                alert.setHeaderText("Password incorrect");
                                alert.setContentText("密码错误 and try again.");
                                alert.showAndWait();
                                try {
                                    socket.close();in.close();
                                    out.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                System.out.println(1);
                                // 在 UI 线程中关闭程序
                                Platform.exit();
                                Thread.currentThread().interrupt(); // 中断线程
                                System.exit(0);

                            });

                            break;
                    }
                }
            }

        }).start();

        Dialog<Pair<String, String>> loginDialog = new Dialog<>();
        loginDialog.setTitle("Login");
        loginDialog.setHeaderText(null);

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        loginDialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // 创建账户和密码输入框
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        loginDialog.getDialogPane().setContent(grid);

        // 设置“Login”按钮的响应事件
        loginDialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {

                return new Pair<>(usernameField.getText(), passwordField.getText());

            }
            return null;
        });

        // 显示对话框，并获取用户输入的账户和密码
        Optional<Pair<String, String>> result = loginDialog.showAndWait();
        if (result.isPresent() && !result.get().getKey().isEmpty() && !result.get().getValue().isEmpty()) {
            // 处理用户输入
            String username = result.get().getKey();
            String password = result.get().getValue();
            Message message=new Message(System.currentTimeMillis(),username,password,username,"", MessageType.ASKFORCONNECT);
            String aaa=Message.toJson(message);
            out.println(aaa);

            out.flush();
        } else {
            // 用户未输入账户或密码，退出应用程序
            System.out.println("Invalid username or password, exiting");
            Platform.exit();
        }
         Message message=new Message(System.currentTimeMillis(),"","","","",MessageType.CONNECT);
        String[] dd=new String[2];
        dd[0]="123";
        dd[1]="253";
        String[] bb=new String[2];
        bb[0]="123";
        bb[1]="";
        message.getChat().a=dd;
        message.getChat().b=bb;

        onlineUsersList.setItems(FXCollections.observableArrayList(users));

        chatContentList.getItems().add(message);


        chatContentList.setItems(messages);
        chatContentList.setCellFactory(new MessageCellFactory());

    }

    public void updateOnlineUsersList(List<String> usernames) {

        onlineUsersList.getItems().clear();


        for (String username : usernames) {
            onlineUsersList.getItems().add(username);
        }
    }

    public void onMessageReceived(Message msg) {
        Platform.runLater(() -> {
            messages.clear();
            messages.add(msg);
        });
    }


    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out
        userSel.getItems().addAll("Item 1", "Item 2", "Item 3");

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        String text = inputArea.getText().trim(); // 获取输入框的文本内容
        if (!text.isEmpty()&chating!=0) { // 确保输入内容不为空且选择过某个聊天
           Message m=new Message(System.currentTimeMillis(),username,"","",text,MessageType.Send);
           m.getChat().setId(chating);
            String s=Message.toJson(m);
            out.println(s);
            out.flush();
            inputArea.clear();
        }
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */

    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {

        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);

                    if (empty || msg == null) {

                        return;
                    }

                    VBox container = new VBox();

                    for (int i = 0; i < msg.getChat().getA().length; i++) {
                        HBox wrapper = new HBox();
                        Label nameLabel = new Label(msg.getChat().getB()[i] );
                        Label msgLabel = new Label(msg.getChat().getA()[i]);


                        nameLabel.setPrefSize(50, 20);
                        nameLabel.setWrapText(true);
                        nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");


                        if (msg.getChat().getB()[i].equals(username)) {
                            wrapper.setAlignment(Pos.TOP_RIGHT);

                            wrapper.getChildren().addAll(msgLabel, nameLabel);
                            nameLabel.setPadding(new Insets(0, 20, 0, 0));
                        } else {
                            System.out.println(i);
                            System.out.println(msg.getChat().getB()[i]);
                            wrapper.setAlignment(Pos.TOP_LEFT);
                            wrapper.getChildren().addAll(nameLabel, msgLabel);
                            nameLabel.setPadding(new Insets(0, 0, 0, 20));
                        }

                        container.getChildren().add(wrapper);
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(container);
                }
            };
        }
    }

}
