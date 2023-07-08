
import network.Client;
import tree_strcture.*;

//Elgamal换成BigInteger实现，BigInteger可以与byte[]相互转化
public class Main {

    //实际计算平均值时需要剔除几个最大值和最小值，以防单机模拟多用户带来的影响
    /** CSV format:
     *  (operationCnt represents the x th operation)
     *  (encTimes is not null when role==sender)
     *  (appendMsg is not null when operation is remove or add)
     *  (Add2 will expand the tree structure, while add1 will not)
     *  groupSize,  operationCnt,   role,       ID, operation,  time(ms),   encTimes,   appendMsg
     *  e.g:    8,  0,              sender,     0,  update,     3.11,       5,          null
     *          16, 5,              recipient,  7,  remove,     2.12,       null,       13
     *          128,27,             sender,     45, add1,       4.51,       1,          35
     *          128,27,             sender,     45, add2,       4.51,       1,          129
     *
     *  CSV file naming convention:
     *  groupsize-totalOperationCnt-operation.csv
     *  e.g:8-100-update.csv
     * */
    //网络交互时延（轮数）
    public static void main(String[] args){
        CGKATest cgkaTest = new CGKATest();

        /** update和remove的效率基本相同
         *  测add的同时可以顺便测create
         *  测试的群组规模有：8/16/32/64/128/256/512
         *  根据测试的规模选择不同的延时时间
         * */
        cgkaTest.test16();

        //cgkaTest.test_update(32);
//        try {
//            Thread.sleep(5000);
//        }catch (Exception e){
//            e.printStackTrace();
//        }


        //cgkaTest.test_create(8);
        //cgkaTest.test_remove(8);
        cgkaTest.test_add1(64);
        //cgkaTest.test_add2(64);
    }



}
