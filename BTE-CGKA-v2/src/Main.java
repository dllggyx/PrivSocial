
import network.Client;
import tree_strcture.*;

//Elgamal换成BigInteger实现，BigInteger可以与byte[]相互转化
public class Main {
    public static void main(String[] args) throws Exception {

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
        clients[0].Create(group);//a creates group
        mySleep(5000);
        clients[0].Update();//a updates
        mySleep(5000);
        //clients[0].Add("iiiii",clients[8].getPkAndSvk());
        //clients[0].Remove("eeeee",clients[4].getPkAndSvk());
        clients[4].Update();//e updates
        mySleep(5000);
        clients[3].Update();//d updates
        mySleep(5000);
        clients[3].Remove(clients[6].ID,clients[6].getPkAndSvk());//d removes g
        mySleep(5000);
        clients[0].Add(clients[8].ID,clients[8].getPkAndSvk());//a adds i
        mySleep(5000);
        clients[1].Add(clients[9].ID,clients[9].getPkAndSvk());//b adds j
        mySleep(5000);
        System.out.println("Hello world!");
        mySleep(5000);

        for(int i=0;i<10;i++){
            clients[i].join();
        }

    }

    public static void mySleep(int time){
        try {
            Thread.sleep(time);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
