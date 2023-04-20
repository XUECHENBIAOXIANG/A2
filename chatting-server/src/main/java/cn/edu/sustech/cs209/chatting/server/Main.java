package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    private static Connection con = null;
    private static String db="jdbc:postgresql://localhost:5432/peo";
    private static String user="postgres";
    private static String pwd="111111";
    private static long use_max_id=1;
    private static ArrayList<PrintWriter> outputStreams = new ArrayList<>();
    public static void main(String[] args) throws IOException, SQLException {
        getConnection();
        findid();
        System.out.println(use_max_id);
        ServerSocket serverSocket=new ServerSocket(8888);
        while (true){
            Socket socket = serverSocket.accept();
            System.out.println(socket.getInetAddress() + "连接上了本服务器");
            new Thread(new ServerThread(socket)).start();
        }

    }

    private static class ServerThread implements Runnable {
        private Socket socket = null;
        private static Scanner in;
        private static PrintWriter out;
        private String User;
        public ServerThread(Socket socket) {
              this.socket=socket;
        }

        @Override
        public void run() {
            try {
                 in=new Scanner(socket.getInputStream());
                 out=new PrintWriter(socket.getOutputStream());
                 outputStreams.add(out);
                doService();
            } catch (SocketException se) { /*处理用户断开的异常*/
                System.out.println("处理用户断开的异常");

            }catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    socket.close();
                    in.close();
                    out.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        public static void doService() throws SQLException {
            getConnection();
            while (true){
                if (in.hasNext()){
                    String line=in.nextLine();
                    Message message= Message.fromJson(line);
                    switch (message.getType()){
                        case ASKFORCONNECT:
                            String zhanghu= message.getSentBy();
                            String mima= message.getMima();
                            String sql="select mima from login where zhanghu=?";
                            String sql1="insert into login(zhanghu,mima) values(?,?)";
                            try {
                                PreparedStatement s1=con.prepareStatement(sql);
                                s1.setString(1,zhanghu);
                                ResultSet resultSet= s1.executeQuery();
                                if (resultSet.next()){
                                    if (resultSet.getString(1).equals(mima)) {
                                        user=zhanghu;
                                        System.out.println("success");
                                        Message message2=new Message(System.currentTimeMillis(),zhanghu,mima,zhanghu,"", MessageType.OtherConnect);
                                        Message message1=new Message(System.currentTimeMillis(),zhanghu,mima,zhanghu,"", MessageType.CONNECT);
                                        String JSON=Message.toJson(message1);//need to improve
                                        for (PrintWriter out : outputStreams) {
                                            out.println(JSON);
                                            out.flush();
                                        }
                                    }else {
                                        Message message1=new Message(System.currentTimeMillis(),zhanghu,mima,zhanghu,"", MessageType.DISCONNECT);
                                        String Json=Message.toJson(message1);
                                        out.println(Json);
                                        System.out.println("don;t");
                                        out.flush();

                                    }
                                }else {
                                    user=zhanghu;
                                    PreparedStatement s2=con.prepareStatement(sql1);
                                    s2.setString(1,zhanghu);
                                    s2.setString(2,mima);
                                    s2.execute();
                                    System.out.println("new");
                                    Message message1=new Message(System.currentTimeMillis(),zhanghu,mima,zhanghu,"", MessageType.CONNECT);
                                    String JSON=Message.toJson(message1);//need to improve
                                    for (PrintWriter out : outputStreams) {
                                        out.println(JSON);
                                        out.flush();
                                    }


                                }


                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            break;
                        case ASKFORCHAT:
                            break;
                        case Send:
                            long a= message.getChat().getId();
                            long timestamp=message.getTimestamp();
                            String send=message.getSentBy();
                            String text=message.getData();
                            String ren=message.getSendTo();
                            String sqlxin="insert into chat_information values (?,?,?,?,?)";
                            PreparedStatement preparedStatement= con.prepareStatement(sqlxin);
                            preparedStatement.setTimestamp(1,Timestamp.valueOf(String.valueOf(timestamp)));
                            preparedStatement.setLong(2,a);
                            preparedStatement.setString(3,send);
                            preparedStatement.setString(4,text);
                            preparedStatement.setString(5,ren);
                            preparedStatement.execute();



                            break;


                    }


                }else {
                    return;
                }
            }
        }
    }
    private static void getConnection() {
        try {
            Class.forName("org.postgresql.Driver");

        } catch (Exception e) {

            System.err.println("Cannot find the PostgreSQL driver. Check CLASSPATH.");
            System.exit(1);
        }

        try {
            String url = db;
            con = DriverManager.getConnection(url, user, pwd);
        } catch (SQLException e) {
            System.err.println("Database connection failed");
            System.err.println(e.getMessage());
            System.exit(1);

        }
    }
    private static void findid() throws SQLException {
        String sql="select max(id)from chat_information";PreparedStatement preparedStatement= con.prepareStatement(sql);
        ResultSet resultSet=preparedStatement.executeQuery();
        if (resultSet.next()){
            use_max_id=resultSet.getLong(1);
        }

    }
    private static void closeConnection() {
        if (con != null) {
            try {
                con.close();
                con = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

