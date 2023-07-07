import network.Client;
import tree_strcture.BinaryTree;
import tree_strcture.Group;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class CGKATest {

    public void test8(){
        Group group = new Group();
        //local user is aaaaa
        System.out.println("key generate start");
        Client[] clients = new Client[10];
        clients[0] = new Client("aaaaa");
        clients[1] = new Client("bbbbb");
        clients[2] = new Client("ccccc");
        clients[3] = new Client("ddddd");
        clients[4] = new Client("eeeee");
        clients[5] = new Client("fffff");
        clients[6] = new Client("ggggg");
        clients[7] = new Client("hhhhh");
        clients[8] = new Client("iiiii");
        clients[9] = new Client("jjjjj");
        System.out.println("key generate end");

        //ArrayList<byte[]> keys= MyElGamal.generateKey("sdsdsd".getBytes());
        //test(keys.get(0),keys.get(1));

        for(int i=0;i<10;i++){
            clients[i].startClient();
            //clients[i].Btree.synchronize(clients[i].getSocket());
        }

        group.addMember(clients[0].ID, clients[0].getFullIdentityKeys());
        group.addMember("bbbbb", clients[1].getPkAndSvk());
        group.addMember("ccccc", clients[2].getPkAndSvk());
        group.addMember("ddddd", clients[3].getPkAndSvk());
        group.addMember("eeeee", clients[4].getPkAndSvk());
        group.addMember("fffff", clients[5].getPkAndSvk());
        group.addMember("ggggg", clients[6].getPkAndSvk());
        group.addMember("hhhhh", clients[7].getPkAndSvk());


        //create the group
        System.out.println("create1");
        clients[0].Create(group);//a creates group
        mySleep(5000);

//        System.out.println("update1");
//        clients[0].Update();//a updates
//        mySleep(5000);
//
//        System.out.println("update2");
//        clients[4].Update();//e updates
//        mySleep(5000);
//
//        System.out.println("update3");
//        clients[3].Update();//d updates
//        mySleep(5000);
//
//        System.out.println("remove1");
//        clients[3].Remove(clients[6].ID,clients[6].getPkAndSvk());//d removes g
//        mySleep(5000);
//
//        System.out.println("add1");
//        clients[0].Add(clients[8].ID,clients[8].getPkAndSvk());//a adds i
//        mySleep(5000);
//
//        System.out.println("add2");
//        clients[1].Add(clients[9].ID,clients[9].getPkAndSvk());//b adds j
//        mySleep(7000);
//        System.out.println("Hello world!");
//        mySleep(5000);

        for(int i=0;i<10;i++){
            clients[i].join();
        }
    }

    public void test16(){
        int GROUP_SIZE = 16 + 2;

        System.out.println("key generate start");
        Client[] clients = new Client[GROUP_SIZE];
        for(int i=0;i<GROUP_SIZE;i++){
            clients[i] = new Client(Integer.toString(i));
        }
        System.out.println("key generate end");

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].startClient();
        }
        Group group = new Group();
        group.addMember(clients[0].ID, clients[0].getFullIdentityKeys());
        for(int i=1;i<GROUP_SIZE - 2;i++){
            group.addMember(Integer.toString(i), clients[i].getPkAndSvk());
        }
        mySleep(5000);
        System.out.println("create1");
        clients[0].Create(group);//a creates group
        mySleep(5000);

        System.out.println("update1");
        clients[7].Update();//a updates
        mySleep(5000);

        System.out.println("update2");
        clients[13].Update();//e updates
        mySleep(5000);

        System.out.println("update3");
        clients[5].Update();//d updates
        mySleep(5000);

        System.out.println("remove1");
        clients[8].Remove(clients[9].ID,clients[9].getPkAndSvk());//d removes g
        mySleep(5000);

        System.out.println("add1");
        clients[14].Add(clients[16].ID,clients[16].getPkAndSvk());//a adds i
        mySleep(5000);

        System.out.println("add2");
        clients[9].Add(clients[17].ID,clients[17].getPkAndSvk());//b adds j
        mySleep(7000);
        System.out.println("Hello world!");
        mySleep(5000);

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].join();
        }
    }

    public void test32(){
        int GROUP_SIZE = 32 + 2;

        System.out.println("key generate start");
        Client[] clients = new Client[GROUP_SIZE];
        for(int i=0;i<GROUP_SIZE;i++){
            clients[i] = new Client(Integer.toString(i));
        }
        System.out.println("key generate end");

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].startClient();
        }
        Group group = new Group();
        group.addMember(clients[0].ID, clients[0].getFullIdentityKeys());
        for(int i=1;i<GROUP_SIZE - 2;i++){
            group.addMember(Integer.toString(i), clients[i].getPkAndSvk());
        }
        mySleep(5000);
        System.out.println("create1");
        clients[0].Create(group);//a creates group
        mySleep(5000);

        System.out.println("update1");
        clients[22].Update();//a updates
        mySleep(5000);

        System.out.println("update2");
        clients[16].Update();//e updates
        mySleep(5000);

        System.out.println("update3");
        clients[5].Update();//d updates
        mySleep(5000);

        System.out.println("remove1");
        clients[29].Remove(clients[9].ID,clients[9].getPkAndSvk());//d removes g
        mySleep(5000);

        System.out.println("add1");
        clients[14].Add(clients[33].ID,clients[33].getPkAndSvk());//a adds i
        mySleep(5000);

        System.out.println("add2");
        clients[3].Add(clients[32].ID,clients[32].getPkAndSvk());//b adds j
        mySleep(7000);
        System.out.println("Hello world!");
        mySleep(5000);

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].join();
        }
    }

    public void test64(){
        int GROUP_SIZE = 64 + 2;

        System.out.println("key generate start");
        Client[] clients = new Client[GROUP_SIZE];
        for(int i=0;i<GROUP_SIZE;i++){
            clients[i] = new Client(Integer.toString(i));
        }
        System.out.println("key generate end");

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].startClient();
        }
        Group group = new Group();
        group.addMember(clients[0].ID, clients[0].getFullIdentityKeys());
        for(int i=1;i<GROUP_SIZE - 2;i++){
            group.addMember(Integer.toString(i), clients[i].getPkAndSvk());
        }
        mySleep(5000);
        System.out.println("create1");
        clients[0].Create(group);//a creates group
        mySleep(5000);

        System.out.println("update1");
        clients[63].Update();//a updates
        mySleep(5000);

        System.out.println("update2");
        clients[5].Update();//e updates
        mySleep(5000);

        System.out.println("update3");
        clients[34].Update();//d updates
        mySleep(5000);

        System.out.println("remove1");
        clients[19].Remove(clients[27].ID,clients[27].getPkAndSvk());//d removes g
        mySleep(5000);

        System.out.println("add1");
        clients[9].Add(clients[64].ID,clients[64].getPkAndSvk());//a adds i
        mySleep(5000);

        System.out.println("add2");
        clients[49].Add(clients[65].ID,clients[65].getPkAndSvk());//b adds j
        mySleep(7000);
        System.out.println("Hello world!");
        mySleep(5000);

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].join();
        }
    }

    public void test128(){
        int GROUP_SIZE = 128 + 2;

        System.out.println("key generate start");
        Client[] clients = new Client[GROUP_SIZE];
        for(int i=0;i<GROUP_SIZE;i++){
            clients[i] = new Client(Integer.toString(i));
        }
        System.out.println("key generate end");

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].startClient();
        }
        Group group = new Group();
        group.addMember(clients[0].ID, clients[0].getFullIdentityKeys());
        for(int i=1;i<GROUP_SIZE - 2;i++){
            group.addMember(Integer.toString(i), clients[i].getPkAndSvk());
        }
        mySleep(30000);
        System.out.println("create1");
        clients[0].Create(group);//a creates group
        mySleep(30000);

        System.out.println("update1");
        clients[63].Update();//a updates
        mySleep(30000);

        System.out.println("update2");
        clients[5].Update();//e updates
        mySleep(30000);

        System.out.println("update3");
        clients[77].Update();//d updates
        mySleep(30000);

        System.out.println("remove1");
        clients[92].Remove(clients[56].ID,clients[56].getPkAndSvk());//d removes g
        mySleep(30000);

        System.out.println("add1");
        clients[120].Add(clients[128].ID,clients[128].getPkAndSvk());//a adds i
        mySleep(30000);

        System.out.println("add2");
        clients[23].Add(clients[129].ID,clients[129].getPkAndSvk());//b adds j
        mySleep(3000);
        System.out.println("Hello world!");
        mySleep(5000);

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].join();
        }
    }

    public void test256(){
        int GROUP_SIZE = 256 + 2;

        System.out.println("key generate start");
        Client[] clients = new Client[GROUP_SIZE];
        for(int i=0;i<GROUP_SIZE;i++){
            clients[i] = new Client(Integer.toString(i));
        }
        System.out.println("key generate end");

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].startClient();
        }
        Group group = new Group();
        group.addMember(clients[0].ID, clients[0].getFullIdentityKeys());
        for(int i=1;i<GROUP_SIZE - 2;i++){
            group.addMember(Integer.toString(i), clients[i].getPkAndSvk());
        }
        mySleep(60000);
        System.out.println("create1");
        clients[0].Create(group);//a creates group
        mySleep(60000);

        System.out.println("update1");
        clients[128].Update();//a updates
        mySleep(60000);

        System.out.println("update2");
        clients[240].Update();//e updates
        mySleep(60000);

        System.out.println("update3");
        clients[33].Update();//d updates
        mySleep(60000);

        System.out.println("remove1");
        clients[34].Remove(clients[6].ID,clients[6].getPkAndSvk());//d removes g
        mySleep(60000);

        System.out.println("add1");
        clients[85].Add(clients[256].ID,clients[256].getPkAndSvk());//a adds i
        mySleep(60000);

        System.out.println("add2");
        clients[110].Add(clients[257].ID,clients[257].getPkAndSvk());//b adds j
        mySleep(60000);
        System.out.println("Hello world!");
        mySleep(5000);

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].join();
        }
    }

    public void test512(){
        int GROUP_SIZE = 512 + 2;

        System.out.println("key generate start");
        Client[] clients = new Client[GROUP_SIZE];
        for(int i=0;i<GROUP_SIZE;i++){
            clients[i] = new Client(Integer.toString(i));
        }
        System.out.println("key generate end");

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].startClient();
        }
        Group group = new Group();
        group.addMember(clients[0].ID, clients[0].getFullIdentityKeys());
        for(int i=1;i<GROUP_SIZE - 2;i++){
            group.addMember(Integer.toString(i), clients[i].getPkAndSvk());
        }
        mySleep(60000);
        System.out.println("create1");
        clients[0].Create(group);//a creates group
        mySleep(90000);

        System.out.println("update1");
        clients[128].Update();//a updates
        mySleep(60000);

        System.out.println("update2");
        clients[240].Update();//e updates
        mySleep(60000);

        System.out.println("update3");
        clients[33].Update();//d updates
        mySleep(60000);

        System.out.println("remove1");
        clients[34].Remove(clients[6].ID,clients[6].getPkAndSvk());//d removes g
        mySleep(60000);

        System.out.println("add1");
        clients[85].Add(clients[512].ID,clients[512].getPkAndSvk());//a adds i
        mySleep(60000);

        System.out.println("add2");
        clients[110].Add(clients[513].ID,clients[513].getPkAndSvk());//b adds j
        mySleep(60000);
        System.out.println("Hello world!");
        mySleep(5000);

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].join();
        }
    }

    public void test_update(int scale){
        if(scale == 8 || scale == 16 || scale == 32 || scale == 64 || scale == 128 || scale == 256)
            return;
        int GROUP_SIZE = scale;
        int EXEC_TIMES = 100;
        String dir = "evaluation/"+GROUP_SIZE+"-"+ EXEC_TIMES +"-update.csv";
        File file = new File(dir);
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(file, true));// true,则追加写入text文本
            output.write("groupSize,operationCnt,role,ID,operation,time(ms),encTimes,appendMsg");//Table header
        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("key generate start");
        Client[] clients = new Client[GROUP_SIZE];
        for(int i=0;i<GROUP_SIZE;i++){
            clients[i] = new Client(Integer.toString(i));
        }
        System.out.println("key generate end");

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].startClient();
        }
        Group group = new Group();
        group.addMember(clients[0].ID, clients[0].getFullIdentityKeys());
        for(int i=1;i<GROUP_SIZE;i++){
            group.addMember(Integer.toString(i), clients[i].getPkAndSvk());
        }
        mySleep(5000);

        //create the group
        System.out.println("create1");
        clients[0].Create(group);//a creates group
        mySleep(60000);

        Random rand = new Random();
        for(int i=0;i<EXEC_TIMES;i++){
            System.out.println("============================"+i+"============================");
            int randIdx = rand.nextInt(GROUP_SIZE);
            clients[randIdx].Update();
            mySleep(60000);
            writeCsv(output,clients,GROUP_SIZE,clients[randIdx].ID,i,"update","null");

            mySleep(1000);
        }
        try {
            output.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        for(int i=0;i<GROUP_SIZE;i++){
            clients[i].join();
        }
    }

    public void test_create(int scale){
        //if(scale == 8 || scale == 16)
        //    return;

        int GROUP_SIZE = scale;
        int EXEC_TIMES = 100;
        int TIME = 3000;
        if(scale == 8 || scale == 16)
            TIME = 3000;
        else if(scale == 32 || scale == 64 || scale == 128)
            TIME = 5000;
        else if(scale == 256)
            TIME = 20000;
        else if(scale == 512)
            TIME = 60000;
        String dir = "evaluation/"+GROUP_SIZE+"-"+ EXEC_TIMES +"-create-test.csv";
        File file = new File(dir);
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(file, true));// true,则追加写入text文本
            output.write("groupSize,operationCnt,role,ID,operation,time(ms),encTimes,appendMsg");//Table header
        }catch (Exception e){
            e.printStackTrace();
        }



        Random rand = new Random();
        for(int i=0;i<EXEC_TIMES;i++){
            System.out.println("============================"+i+"============================");
            System.out.println("key generate start");
            Client[] clients = new Client[GROUP_SIZE];
            for(int j=0;j<GROUP_SIZE;j++){
                clients[j] = new Client(Integer.toString(j));
            }
            System.out.println("key generate end");

            for(int j=0;j<GROUP_SIZE;j++){
                clients[j].startClient();
            }
            mySleep(TIME);
            int randIdx = rand.nextInt(GROUP_SIZE);
            Group group = new Group();
            for(int j=0;j<GROUP_SIZE;j++){
                if(j == randIdx)
                    group.addMember(clients[randIdx].ID, clients[randIdx].getFullIdentityKeys());
                else
                    group.addMember(Integer.toString(j), clients[j].getPkAndSvk());
            }
            mySleep(TIME);
            //create the group
            System.out.println("create1");
            clients[randIdx].Create(group);//a creates group
            mySleep(TIME);
            writeCsv(output,clients,GROUP_SIZE,clients[randIdx].ID,i,"create","null");
            for(int j=0;j<GROUP_SIZE;j++){
                clients[j].join();
            }
            mySleep(TIME/2);
        }
        System.out.println("test end");
        try {
            output.close();
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public void test_add1(int scale){
        //from scale-1 -> scale
        int GROUP_SIZE = scale;
        int ADD_SIZE = GROUP_SIZE - 1;
        int EXEC_TIMES = 50;
        String dir = "evaluation/"+GROUP_SIZE+"-"+ EXEC_TIMES +"-add1.csv";
        File file = new File(dir);
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(file, true));// true,则追加写入text文本
            output.write("groupSize,operationCnt,role,ID,operation,time(ms),encTimes,appendMsg");//Table header
        }catch (Exception e){
            e.printStackTrace();
        }


        Random rand = new Random();
        for(int i=0;i<EXEC_TIMES;i++){
            System.out.println("============================"+i+"============================");
            System.out.println("key generate start");
            Client[] clients = new Client[GROUP_SIZE];
            for(int j=0;j<GROUP_SIZE;j++){
                clients[j] = new Client(Integer.toString(j));
            }
            System.out.println("key generate end");

            for(int j=0;j<GROUP_SIZE;j++){
                clients[j].startClient();
            }
            mySleep(3000);
            int randIdx = rand.nextInt(ADD_SIZE);
            Group group = new Group();
            for(int j=0;j<ADD_SIZE;j++){
                if(j == randIdx)
                    group.addMember(clients[randIdx].ID, clients[randIdx].getFullIdentityKeys());
                else
                    group.addMember(Integer.toString(j), clients[j].getPkAndSvk());
            }
            mySleep(5000);
            //create the group
            clients[randIdx].Create(group);//a creates group
            mySleep(3000);
            clients[randIdx].Add(clients[ADD_SIZE].ID,clients[ADD_SIZE].getPkAndSvk());
            mySleep(3000);
            writeCsv(output,clients,GROUP_SIZE,clients[randIdx].ID,i,"add",clients[ADD_SIZE].ID);
            for(int j=0;j<GROUP_SIZE;j++){
                clients[j].join();
            }
            mySleep(1000);
        }
        System.out.println("test end");
        try {
            output.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void test_add2(int scale){
        //from scale -> scale+1
        int GROUP_SIZE = scale;
        int EXEC_TIMES = 50;
        String dir = "evaluation/"+GROUP_SIZE+"-"+ EXEC_TIMES +"-add2.csv";
        File file = new File(dir);
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(file, true));// true,则追加写入text文本
            output.write("groupSize,operationCnt,role,ID,operation,time(ms),encTimes,appendMsg");//Table header
        }catch (Exception e){
            e.printStackTrace();
        }


        Random rand = new Random();
        for(int i=0;i<EXEC_TIMES;i++){
            System.out.println("============================"+i+"============================");
            System.out.println("key generate start");
            Client[] clients = new Client[GROUP_SIZE+1];
            for(int j=0;j<GROUP_SIZE+1;j++){
                clients[j] = new Client(Integer.toString(j));
            }
            System.out.println("key generate end");

            for(int j=0;j<GROUP_SIZE+1;j++){
                clients[j].startClient();
            }
            mySleep(3000);
            int randIdx = rand.nextInt(GROUP_SIZE);
            Group group = new Group();
            for(int j=0;j<GROUP_SIZE;j++){
                if(j == randIdx)
                    group.addMember(clients[randIdx].ID, clients[randIdx].getFullIdentityKeys());
                else
                    group.addMember(Integer.toString(j), clients[j].getPkAndSvk());
            }
            mySleep(5000);
            //create the group
            clients[randIdx].Create(group);//a creates group
            mySleep(3000);
            clients[randIdx].Add(clients[GROUP_SIZE].ID,clients[GROUP_SIZE].getPkAndSvk());
            mySleep(3000);
            writeCsv(output,clients,GROUP_SIZE,clients[randIdx].ID,i,"add",clients[GROUP_SIZE].ID);
            for(int j=0;j<GROUP_SIZE;j++){
                clients[j].join();
            }
            mySleep(1000);
        }
        System.out.println("test end");
        try {
            output.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void test_remove(int scale){
        //多了个置空的过程
    }

    public static void mySleep(int time){
        try {
            Thread.sleep(time);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void myWait(Client[] clients){
        for(Client client:clients){
            //judge isFine
        }
    }

    public void writeCsv(BufferedWriter output,Client[] clients,int groupSize,String senderID,int operationCnt,String operation,String appendMsg){
        try {

            for (Client c : clients) {
                output.write("\r\n");// 换行
                //groupSize,operationCnt,role,ID,operation,time(ms),encTimes,appendMsg
                if(c.ID.equals(senderID))
                    output.write(groupSize + "," + operationCnt + "," +"sender" + "," + c.ID + "," + operation + "," + c.Btree.clientTime + "," + c.Btree.encTimes + "," + appendMsg);
                else
                    output.write(groupSize + "," + operationCnt + "," +"recipient" + "," + c.ID + "," + operation + "," + c.Btree.clientTime + "," + "null," + appendMsg);
            }
            output.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}