package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


//只取前16位
public class Hash1
{

    /**
     * 传入文本内容，返回 SHA-256 串
     *
     * @param strText
     * @return
     */
    public byte[] SHA256(final byte[] strText)
    {
        return SHA(strText, "SHA-256");
    }

    private byte[] SHA(final byte[] strText, final String strType)
    {
        byte []byteBuffer = null;
        // 是否是有效字符串
        if (strText != null && strText.length > 0)
        {
            try
            {
                // SHA 加密开始
                // 创建加密对象 并傳入加密類型
                MessageDigest messageDigest = MessageDigest.getInstance(strType);
                // 传入要加密的字符串
                messageDigest.update(strText);
                // 得到 byte 類型结果
                byteBuffer = messageDigest.digest();

            }
            catch (NoSuchAlgorithmException e)
            {
                e.printStackTrace();
            }
        }
        byte []result = new byte[16];
        if(byteBuffer != null) {
            System.arraycopy(byteBuffer, 0, result, 0, 16);
            return result;
        }
        else
            return null;

        //return byteBuffer;
    }
}