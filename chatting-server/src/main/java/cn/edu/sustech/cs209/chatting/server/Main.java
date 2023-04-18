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
    private static ArrayList<PrintWriter> outputStreams = new ArrayList<>();
    public static void main(String[] args) throws IOException {
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
                    closeConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        public static void doService(){
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
                                        out.flush();

                                    }
                                }else {
                                    user=zhanghu;
                                    PreparedStatement s2=con.prepareStatement(sql1);
                                    s2.setString(1,zhanghu);
                                    s2.setString(2,mima);
                                    s2.execute();
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
                        case ASKFORCHAT:


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

