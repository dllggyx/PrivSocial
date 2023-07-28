package utils;

import key.IdentityKeys;
import key.PublicKeyPair;
import key.SignKeyPair;
import network.Information;
import tree_strcture.ServerTree;

import java.math.BigInteger;

public class MyUtils {

    public static final Integer myLock = 0;

    public static void Sprocess(ServerTree sTree, Information info){
        switch (info.tag){
            case "init":
                SCreate(sTree,info);
                break;
            case "update":
                SUpdate(sTree,info);
                break;
            case "add":
                SAdd(sTree,info);
                break;
            case "remove":
                SRemove(sTree,info);
                break;
            default:
                break;
        }
    }

    public static void SCreate(ServerTree sTree, Information info){
        sTree.create(info.group, info.senderID);
    }

    public static void SUpdate(ServerTree sTree, Information info){
        sTree.update(info.senderID, info.senderPk, info.senderSvk);
    }

    public static void SAdd(ServerTree sTree, Information info){
        //sTree.add(info.senderID,xxx);
    }

    public static void SRemove(ServerTree sTree, Information info){
        //sTree.remove(info.senderID,xxx);
    }

    public static void CCreate(){

    }

    public static void sendInformation(Information info){

    }

}
