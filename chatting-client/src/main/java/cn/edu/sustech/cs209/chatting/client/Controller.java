package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {
    private List<String> chat_users = new ArrayList<>();
    private List<String> online=new ArrayList<>();
    @FXML Label currentUsername;
    @FXML Label currentOnlineCnt;
    @FXML
    ListView<String> chatList;
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
    Long chating= 9999l;

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
            try {
                while (!Thread.interrupted()) {  // 修改while条件，当线程被中断时跳出循环
                    if (in!=null&&in.hasNext()){
                        String line=in.nextLine();
                        Message message= Message.fromJson(line);

                        switch (message.getType()){
                            case  CONNECT:

                                Platform.runLater(()->{
                                    username=message.getSendTo();
                                    chat_users=message.getPeople();
                                    online=message.getAllchat();
                                    chatList.setItems(FXCollections.observableArrayList(online));
                                    currentOnlineCnt.setText("Online:"+chat_users.size());
                                    currentUsername.setText("Current User: "+username);
                                    onlineUsersList.setItems(FXCollections.observableArrayList(chat_users));

                                });

                                break;
                            case OtherConnect:

                                Platform.runLater(()->{ chat_users=message.getPeople();
                                    currentOnlineCnt.setText("Online:"+chat_users.size());
                                    onlineUsersList.setItems(FXCollections.observableArrayList(chat_users));
                                });
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
                            case Receive:

                                Platform.runLater(()-> {
                               /* chat_users .clear();
                                chat_users=message.getAllchat();*/
                                    if (!message.getData().equals("1")){
                                    online.clear();
                                    online.addAll(message.getAllchat());
                                    System.out.println(message.getAllchat());
                                    chatList.setItems(FXCollections.observableArrayList(online));}
                                    if (message.getChat().getId()==chating){
                                        Platform.runLater(() -> {
                                            messages.clear();
                                            messages.add(message);
                                            chatContentList.setItems(messages);
                                            chatContentList.setCellFactory(new MessageCellFactory());
                                        });
                                    }
                                } );


                                break;
                            case GetChat:
                                chating=message.getChat().getId();
                                Platform.runLater(() ->{
                                    messages.clear();
                                    messages.add(message);
                                    online.clear();
                                    online.addAll(message.getAllchat());

                                    chatContentList.setItems(messages);
                                    chatContentList.setCellFactory(new MessageCellFactory());
                                    chatList.setItems(FXCollections.observableArrayList(online));
                                });
                                break;
                            case Receivefile:
                                String encodedFile=message.getData();
                                String fileName=message.getMima();
                                byte[] fileContent = Base64.getDecoder().decode(encodedFile);
                                Files.write(Paths.get(fileName), fileContent);

                                break;

                        }
                    }else if(in==null) {
                        System.out.println("服务器未开");
                        break;
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
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
        chat_users.add("qq");
        chat_users.add("qqq");
        chatList.setItems(FXCollections.observableArrayList(online));


        onlineUsersList.setItems(FXCollections.observableArrayList(chat_users));


        chatList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                String selectedItem = chatList.getSelectionModel().getSelectedItem();
                if (selectedItem!=null) {
                    Message message = new Message(System.currentTimeMillis(), "", "", "", "1", MessageType.newchat);
                    List<String> a = Arrays.asList(selectedItem.split(",", -1));
                    message.setAskchat(a);
                    String x = Message.toJson(message);
                    out.println(x);
                    out.flush();
                }
            }
        });

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
        AtomicReference<String> user1 = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out
        userSel.getItems().addAll(chat_users);
        userSel.getItems().remove(username);


        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user1.set(userSel.getSelectionModel().getSelectedItem());

            Message message=new Message(System.currentTimeMillis(),"","","","",MessageType.newchat);
            List<String> a=new ArrayList<>();
            a.add(user1.get());
            if (a.get(0)==null){
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("renshubugou");
                alert.setContentText("人数不够，请选择至少一人");
                alert.showAndWait();
            }else {

                a.add(username);
                message.setAskchat(a);
                String x=Message.toJson(message);
                out.println(x);
                out.flush();
                stage.close();
            }

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
        // 获取所有在线用户列表，假设为List<String> allUsers
        List<String> allUsers = chat_users;

        // 弹出窗口，让用户选择要邀请的人
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox vBox = new VBox();
        vBox.setPadding(new Insets(20));

        Label label = new Label("Select users to join the group chat:");
        vBox.getChildren().add(label);

        // 创建一个 CheckBox 列表，用于选择用户
        List<CheckBox> checkBoxList = new ArrayList<>();
        for (String user : allUsers) {
            if (!user.equals(username)){
            CheckBox checkBox = new CheckBox(user);
            checkBoxList.add(checkBox);
            vBox.getChildren().add(checkBox);}
        }

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            // 获取选择的用户，创建群聊
            int x=1;
            List<String> selectedUsers = new ArrayList<>();
            for (CheckBox checkBox : checkBoxList) {
                if (checkBox.isSelected()) {x++;
                    selectedUsers.add(checkBox.getText());
                }
            }
             if (x>2){
                    selectedUsers.add(username);
                 Message message=new Message(System.currentTimeMillis(),"","","","",MessageType.newchat);
                 message.setAskchat(selectedUsers);
                 String y=Message.toJson(message);
                 out.println(y);
                 out.flush();

             }else {
                 Alert alert = new Alert(Alert.AlertType.ERROR);
                 alert.setTitle("Error");
                 alert.setHeaderText("renshubugou");
                 alert.setContentText("人数不够，请选择至少两人");
                 alert.showAndWait();
             }
            // 在这里创建群聊，将 selectedUsers 传入即可

            stage.close();
        });
        vBox.getChildren().add(okBtn);

        Scene scene = new Scene(vBox);
        stage.setScene(scene);
        stage.showAndWait();
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
        if (!text.isEmpty()&chating!=9999l) { // 确保输入内容不为空且选择过某个聊天
           Message m=new Message(System.currentTimeMillis(),username,"","",text,MessageType.Send);
           m.getChat().setId(chating);
            String s=Message.toJson(m);
            out.println(s);
            out.flush();
            inputArea.clear();
        }
    }

    public void doUploadFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(new  Stage () );
        if (selectedFile != null) {
            try {
                byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                System.out.println(fileContent);
                String encodedFile = Base64.getEncoder().encodeToString(fileContent);
                System.out.println(encodedFile);
                String x=selectedFile.getName();
                Message m=new Message(System.currentTimeMillis(),username,x,"",encodedFile,MessageType.Sendfile);
                m.getChat().setId(chating);
                String s=Message.toJson(m);
                out.println(s);
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void doDownloadFile(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Download File");
        dialog.setHeaderText(null);
        dialog.setContentText("Please enter the file name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String fileName = result.get();

            // send request to server
            Message m=new Message(System.currentTimeMillis(),username,"","",fileName,MessageType.askfile);
            String s=Message.toJson(m);
            out.println(s);
            out.flush();


        }
    }

    public void showEmojiSelector(ActionEvent actionEvent) {

            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Select Emoji");
            dialog.setHeaderText("Please select an emoji:");
            ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

            ListView<String> listView = new ListView<>();
        listView.setCellFactory(param -> new EmojiCell());
            ObservableList<String> emojiList = FXCollections.observableArrayList("😊", "😂", "👍", "❤ ","😂", "😊", "👍", "👎", "🤔", "😘", "😍", "🤩", "🙏", "👋", "💪", "🤢", "🤮", "🤯", "😱", "😴", "😷", "🤒", "🥺", "👀");
            listView.setItems(emojiList);
            listView.getSelectionModel().selectFirst();

            dialog.getDialogPane().setContent(listView);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == okButtonType) {
                    return listView.getSelectionModel().getSelectedItem();
                }
                return null;
            });

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                inputArea.appendText(result.get());
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
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    VBox container = new VBox();

                    for (int i = 0; i < msg.getChat().getA().length; i++) {
                        HBox wrapper = new HBox();
                        Label nameLabel = new Label(msg.getChat().getB()[i] );
                        Label msgLabel = new Label(msg.getChat().getA()[i]);


                        nameLabel.setPrefSize(50, 20);
                        nameLabel.setWrapText(true);
                        nameLabel.setStyle("-fx-font-weight: bold;");
                        Font font = Font.font("Segoe UI Emoji", FontWeight.NORMAL,26);

                        String message = msg.getChat().getA()[i].replaceAll("\\\\u([0-9A-Fa-f]{4})", "&#x$1;");
                        message = StringEscapeUtils.unescapeHtml4(message);

                        msgLabel.setFont(font);
                        msgLabel.setText(message);
                        msgLabel.setPadding(new Insets(5));
                        msgLabel.setStyle("-fx-background-color: #efefef; -fx-background-radius: 5px;");

                        if (msg.getChat().getB()[i].equals(username)) {
                            wrapper.setAlignment(Pos.TOP_RIGHT);
                            nameLabel.setPadding(new Insets(0, 10, 0, 0));
                            msgLabel.setPadding(new Insets(5, 15, 5, 15));
                            msgLabel.setStyle("-fx-background-color: #007bff; -fx-background-radius: 5px; -fx-text-fill: white;");
                            msgLabel.setAlignment(Pos.CENTER_RIGHT);
                            wrapper.getChildren().addAll(msgLabel, nameLabel);
                            /*nameLabel.setPadding(new Insets(0, 20, 0, 0));*/
                        } else {
                            nameLabel.setPadding(new Insets(0, 0, 0, 10));
                            msgLabel.setPadding(new Insets(5, 15, 5, 15));
                            msgLabel.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5px;");
                            wrapper.setAlignment(Pos.TOP_LEFT);
                            wrapper.getChildren().addAll(nameLabel, msgLabel);
                            /*nameLabel.setPadding(new Insets(0, 0, 0, 20));*/
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
