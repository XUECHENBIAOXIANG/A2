package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Chat;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Main {
    private static Connection con = null;
    private static String db="jdbc:postgresql://localhost:5432/peo";
    private static String user="postgres";
    private static String pwd="111111";
    private static long use_max_id=1;
    private static ConcurrentHashMap<String, Boolean> denglv=new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String,PrintWriter> shuchu=new ConcurrentHashMap<>();
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
        private  Socket socket = null;
        private  Scanner in;
        private  PrintWriter out;
        private  String User;
        public ServerThread(Socket socket) {
              this.socket=socket;

        }

        @Override
        public void run() {
            try {
                 in=new Scanner(socket.getInputStream());
                 out=new PrintWriter(socket.getOutputStream());
                 outputStreams.add(out);

                doService(in,out,socket);
            } catch (SocketException se) { /*处理用户断开的异常*/

                System.out.println("处理用户断开的异常");

            }catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    in.close();
                    out.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        public void doService(Scanner in, PrintWriter out,Socket socket) throws SQLException {
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
                            if(denglv.containsKey(zhanghu)){
                                Message message1=new Message(System.currentTimeMillis(),zhanghu,mima,zhanghu,"", MessageType.yijing);
                                String Json=Message.toJson(message1);
                                out.println(Json);

                                out.flush();
                            }else {
                                try {
                                    PreparedStatement s1 = con.prepareStatement(sql);
                                    s1.setString(1, zhanghu);
                                    ResultSet resultSet = s1.executeQuery();
                                    if (resultSet.next()) {
                                        if (resultSet.getString(1).equals(mima)) {
                                            User = zhanghu;
                                            System.out.println("success");
                                            denglv.put(User, true);
                                            shuchu.put(User, out);
                                            Message message2 = new Message(System.currentTimeMillis(), zhanghu, mima, zhanghu, "", MessageType.OtherConnect);
                                            Message message1 = new Message(System.currentTimeMillis(), zhanghu, mima, zhanghu, "", MessageType.CONNECT);
                                            List<String> a = new ArrayList<>();
                                            a.addAll(denglv.keySet());
                                            System.out.println(a);
                                            message1.setPeople(a);
                                            message2.setPeople(a);


                                            String sql111 = "select chatwho,max(sendtime)as time from chat_information where chatwho like ? group by chatwho order by time desc";
                                            PreparedStatement preparedStatement2 = con.prepareStatement(sql111);
                                            preparedStatement2.setString(1, '%' + User + '%');
                                            ResultSet resultSet1 = preparedStatement2.executeQuery();
                                            List<String> cList = new ArrayList<>();
                                            while (resultSet1.next()) {
                                                String chatwho = resultSet1.getString("chatwho");
                                                String[] temp = chatwho.split(",", -1);
                                                if (Arrays.stream(temp).anyMatch(User::equals)) {
                                                    cList.add(chatwho);
                                                }


                                            }
                                            message1.setAllchat(cList);
                                            String JSON = Message.toJson(message2);//need to improve
                                            String JSON1 = Message.toJson(message1);


                                            for (PrintWriter all_out : outputStreams) {
                                                if (out != all_out) {
                                                    all_out.println(JSON);
                                                    all_out.flush();
                                                } else {
                                                    all_out.println(JSON1);
                                                    all_out.flush();
                                                }
                                            }
                                        } else {
                                            Message message1 = new Message(System.currentTimeMillis(), zhanghu, mima, zhanghu, "", MessageType.DISCONNECT);
                                            String Json = Message.toJson(message1);
                                            out.println(Json);
                                            System.out.println("don;t");
                                            out.flush();

                                        }
                                    } else {
                                        User = zhanghu;
                                        PreparedStatement s2 = con.prepareStatement(sql1);
                                        s2.setString(1, zhanghu);
                                        s2.setString(2, mima);
                                        s2.execute();
                                        System.out.println("new");
                                        denglv.put(User, true);
                                        shuchu.put(User, out);
                                        Message message2 = new Message(System.currentTimeMillis(), zhanghu, mima, zhanghu, "", MessageType.OtherConnect);
                                        Message message1 = new Message(System.currentTimeMillis(), zhanghu, mima, zhanghu, "", MessageType.CONNECT);
                                        List<String> a = new ArrayList<>();
                                        a.addAll(denglv.keySet());
                                        System.out.println(a);
                                        message1.setPeople(a);
                                        message2.setPeople(a);


                                        String sql111 = "select chatwho,max(sendtime)as time from chat_information where chatwho like ? group by chatwho order by time desc";
                                        PreparedStatement preparedStatement2 = con.prepareStatement(sql111);
                                        preparedStatement2.setString(1, '%' + User + '%');
                                        ResultSet resultSet1 = preparedStatement2.executeQuery();
                                        List<String> cList = new ArrayList<>();
                                        while (resultSet1.next()) {
                                            String chatwho = resultSet1.getString("chatwho");
                                            String[] temp = chatwho.split(",", -1);
                                            if (Arrays.stream(temp).anyMatch(User::equals)) {
                                                cList.add(chatwho);
                                            }
                                        }
                                        message1.setAllchat(cList);
                                        String JSON = Message.toJson(message2);//need to improve
                                        String JSON1 = Message.toJson(message1);
                                        for (PrintWriter all_out : outputStreams) {
                                            if (out != all_out) {
                                                all_out.println(JSON);
                                                all_out.flush();
                                            } else {
                                                all_out.println(JSON1);
                                                all_out.flush();
                                            }
                                        }


                                    }


                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;

                        case Send:
                            long a= message.getChat().getId();
                            String send=message.getSentBy();
                            String text=message.getData();
                            String sqlxin="insert into chat_information values (?,?,?,?,?)";
                            String findwho="select chatwho from chat_information where id=?";
                            PreparedStatement preparedStatementwho= con.prepareStatement(findwho);
                            preparedStatementwho.setLong(1,a);
                            ResultSet whoo= preparedStatementwho.executeQuery();
                            String wholist = null;
                            if (whoo.next()){
                                wholist=whoo.getString(1);
                            }
                            PreparedStatement preparedStatement= con.prepareStatement(sqlxin);
                            Timestamp timestamp1 = new Timestamp(message.getTimestamp());
                            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
                            preparedStatement.setTimestamp(1,Timestamp.valueOf(df.format(timestamp1)));
                            preparedStatement.setLong(2,a);
                            preparedStatement.setString(3,send);
                            preparedStatement.setString(4,text);
                            preparedStatement.setString(5,wholist);
                            preparedStatement.execute();
                            String who[]=wholist.split(",",-1);
                            String sql12="select send_by,text from chat_information where id=? order by sendtime asc ";
                            PreparedStatement preparedStatement1= con.prepareStatement(sql12);
                            preparedStatement1.setLong(1,a);
                            ResultSet resultSet=preparedStatement1.executeQuery();
                            List<String> aList = new ArrayList<>();
                            List<String> bList = new ArrayList<>();
                            while (resultSet.next()) {
                                String sendBy = resultSet.getString("send_by");
                                String text1 = resultSet.getString("text");
                                if (!text1.equals("")){
                                    aList.add(text1);
                                    bList.add(sendBy);
                                }

                            }
                            Chat chat = new Chat();
                            chat.setA(aList.toArray(new String[0]));
                            chat.setB(bList.toArray(new String[0]));
                            Message dd=new Message(System.currentTimeMillis(),send,"","","",MessageType.Receive);
                            dd.setChat(chat);
                            chat.setId(a);
                            for (String person:who){
                                System.out.println(person);
                                if (denglv.containsKey(person)){
                                    System.out.println(person);
                                    String sql111="select chatwho,max(sendtime)as time from chat_information where chatwho like ?group by chatwho order by time desc";
                                    PreparedStatement preparedStatement2=con.prepareStatement(sql111);
                                    preparedStatement2.setString(1,'%'+person+'%');
                                    ResultSet resultSet1=preparedStatement2.executeQuery();
                                    List<String> cList = new ArrayList<>();
                                    while (resultSet1.next()) {
                                        String chatwho=resultSet1.getString("chatwho");
                                        String[]temp= chatwho.split(",",-1);
                                        if (Arrays.stream(temp).anyMatch(person::equals)){
                                            cList.add(chatwho);
                                        }

                                    }
                                    dd.setAllchat(cList);
                                    PrintWriter printWriter=shuchu.get(person);
                                    if (person.equals(User)){
                                        dd.setType(MessageType.myreceive);
                                        String ppp=Message.toJson(dd);
                                        printWriter.println(ppp);
                                        printWriter.flush();
                                        dd.setType(MessageType.Receive);
                                    }else {
                                        String ppp=Message.toJson(dd);
                                        printWriter.println(ppp);
                                        printWriter.flush();
                                    }


                                }
                            }
                            break;
                        case newchat:
                            List<String> ddd = message.getAskchat();
                            String sql11111="SELECT id,chatwho " +
                                    "FROM chat_information\n" +
                                    "WHERE chatwho LIKE '%' || ? || '%' \n";
                            for (int i=0;i<ddd.size()-1;i++){
                                sql11111=sql11111+"  AND chatwho LIKE '%' || ? || '%'";
                            }
                            sql11111=sql11111+"and  LENGTH(chatwho) - LENGTH(REPLACE(chatwho, ',', ''))+1=?";

                            PreparedStatement stmt = con.prepareStatement(sql11111);
                            for (int i = 0; i < ddd.size(); i++) {
                                stmt.setString(i + 1, ddd.get(i));
                            }
                            stmt.setLong(ddd.size()+1,ddd.size());

                            System.out.println(stmt);
                            Long id=use_max_id;
                            ResultSet rs = stmt.executeQuery();




String yong = null;
                            if (rs.next()){
                                boolean you=false;
                                String chatwho = rs.getString("chatwho");
                                String[]temp= chatwho.split(",",-1);
                                if (ddd.stream().allMatch(s -> Arrays.asList(temp).contains(s))){
                                    you=true;
                                    yong=chatwho;
                                    id=rs.getLong(1);
                                }else{
                                    while (rs.next()){
                                        String chatwho1 = rs.getString("chatwho");
                                        String[]temp1= chatwho1.split(",",-1);
                                        if (ddd.stream().allMatch(s -> Arrays.asList(temp1).contains(s))){
                                            you=true;
                                            yong=chatwho1;
                                            id=rs.getLong(1);
                                        }
                                    }
                                }
 if (you){

 }else {
     System.out.println("need");
     yong=String.join(",", ddd);
 }


                                String sqqq="INSERT INTO chat_information values(?,?,?,?,?)";
                                PreparedStatement preparedStatement2=con.prepareStatement(sqqq);
                                Timestamp timestamp1q = new Timestamp(message.getTimestamp());
                                SimpleDateFormat dfq = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
                                preparedStatement2.setTimestamp(1,Timestamp.valueOf(dfq.format(timestamp1q)));
                                preparedStatement2.setLong(2,id);
                                preparedStatement2.setString(3,"");
                                preparedStatement2.setString(4,"");
                                preparedStatement2.setString(5,yong);
                                preparedStatement2.execute();
                                if (!you){
                                    use_max_id++;
                                }
                            }else {
                                System.out.println("bukeneng");
                                String chatwho = String.join(",", ddd);
                                String sqqq="INSERT INTO chat_information values(?,?,?,?,?)";
                                PreparedStatement preparedStatement2=con.prepareStatement(sqqq);
                                Timestamp timestamp1q = new Timestamp(message.getTimestamp());
                                SimpleDateFormat dfq = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
                                preparedStatement2.setTimestamp(1,Timestamp.valueOf(dfq.format(timestamp1q)));
                                preparedStatement2.setLong(2,use_max_id);
                                preparedStatement2.setString(3,"");
                                preparedStatement2.setString(4,"");
                                preparedStatement2.setString(5,chatwho);
                                preparedStatement2.execute();
                                id=use_max_id;
                                System.out.println(id);
                                use_max_id++;

                            }
                            System.out.println(id);
                            String sql13="select send_by,text from chat_information where id=? order by sendtime asc ";
                            PreparedStatement preparedStatement9= con.prepareStatement(sql13);
                            preparedStatement9.setLong(1,id);
                            ResultSet resultSet1=preparedStatement9.executeQuery();
                            List<String> cList = new ArrayList<>();
                            List<String> dList = new ArrayList<>();
                            while (resultSet1.next()) {
                                String sendBy = resultSet1.getString("send_by");
                                String text1 = resultSet1.getString("text");
                                if (!text1.equals("")){
                                    System.out.println(text1);
                                    cList.add(text1);
                                    dList.add(sendBy);

                                }

                            }
                            Chat chat1 = new Chat();
                            chat1.setA(cList.toArray(new String[0]));
                            chat1.setB(dList.toArray(new String[0]));
                            Message ee=new Message(System.currentTimeMillis(),"","","","",MessageType.GetChat);
                            Message ff=new Message(System.currentTimeMillis(),"","","","",MessageType.Receive);
                            ff.setChat(chat1);
                            ee.setChat(chat1);
                            chat1.setId(id);
                            for (String person:ddd){

                                if (denglv.containsKey(person)){
                                    String sql111="select chatwho,sendtime from chat_information where chatwho like ? order by sendtime desc";
                                    PreparedStatement preparedStatement2=con.prepareStatement(sql111);
                                    preparedStatement2.setString(1,"%" + person + "%");
                                    ResultSet resultSet2=preparedStatement2.executeQuery();
                                    List<String> gList = new ArrayList<>();
                                    while (resultSet2.next()) {
                                        String chatwholl=resultSet2.getString("chatwho");
                                        String[]temp= chatwholl.split(",",-1);
                                        if (Arrays.stream(temp).anyMatch(person::equals)){
                                            gList.add(chatwholl);
                                        }
                                    }
                                    Set<String> set = new LinkedHashSet<>(gList);
                                    List<String> fList = new ArrayList<>(set);
                                    ee.setAllchat(fList);
                                    ff.setAllchat(fList);
                                    PrintWriter printWriter=shuchu.get(person);

                                    if (person.equals(User)){
                                        System.out.println(person);
                                        String ppp=Message.toJson(ee);
                                        printWriter.println(ppp);
                                        printWriter.flush();
                                        System.out.println(ee.getAllchat());
                                    }else {
                                        if (message.getData().equals("1")){
                                            ff.setData("1");
                                        }
                                        System.out.println(person);
                                        System.out.println(ff.getAllchat());
                                        String ppp=Message.toJson(ff);
                                        printWriter.println(ppp);
                                        printWriter.flush();
                                    }
                                }
                                }
                                break;
                        case Sendfile:
                            String fileName =message.getMima();
                            System.out.println(fileName);
                            String encodedFile = message.getData();
                            System.out.println(encodedFile);
                            byte[] fileContent = Base64.getDecoder().decode(encodedFile);
                            try {
                                Files.write(Paths.get(fileName), fileContent);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            long rooo=message.getChat().getId();
                            String wf="insert into chat_information values (?,?,?,?,?)";
                            String findwho1="select chatwho from chat_information where id=?";
                            PreparedStatement preparedStatementwho1= con.prepareStatement(findwho1);
                            preparedStatementwho1.setLong(1,rooo);
                            ResultSet whoo1= preparedStatementwho1.executeQuery();
                            String wholist1 = null;
                            if (whoo1.next()){
                                wholist1=whoo1.getString(1);
                            }
                            PreparedStatement wf1= con.prepareStatement(wf);
                            Timestamp timestamp11 = new Timestamp(message.getTimestamp());
                            SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
                            wf1.setTimestamp(1,Timestamp.valueOf(df1.format(timestamp11)));
                            wf1.setLong(2,rooo);
                            wf1.setString(3,message.getSentBy());
                            wf1.setString(4,"我上传了 "+fileName);
                            wf1.setString(5,wholist1);
                            wf1.execute();

                            break;
                        case askfile:
                            File file = new File(message.getData());
                            try {
                                byte[] fileContent1 = Files.readAllBytes(file.toPath());
                                String encodedFile1 = Base64.getEncoder().encodeToString(fileContent1);
                                Message m=new Message(System.currentTimeMillis(),"","fuben"+message.getData(),"",encodedFile1,MessageType.Receivefile);
                                String s=Message.toJson(m);
                                out.println(s);
                                out.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }catch (Exception ddddd){
                                ddddd.printStackTrace();
                            }



                    }


                }else {
                    if (User!=null){
                    denglv.remove(User);
                    shuchu.remove(User);
                    outputStreams.remove(out);
                    Message message2=new Message(System.currentTimeMillis(),"","","","", MessageType.OtherConnect);
                    List<String> a=new ArrayList<>();
                    a.addAll(denglv.keySet());
                    message2.setPeople(a);
                    String JSON=Message.toJson(message2);//need to improve
                    for (PrintWriter outputStream:outputStreams){
                        outputStream.println(JSON);
                        outputStream.flush();
                    }
                    System.out.println(User+"断开连接");

                   in.close();
                   out.close();
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    }
                    break;
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
            use_max_id=resultSet.getLong(1)+1;
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

