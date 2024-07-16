#### DCT-test1.py
用于验证dct变换的可逆性，具体函数功能如下：
dct_block:分块做dct-->异或加密-->idct操作
idct_block:分块做idct-->异或解密-->dct操作
block_float32_encrypt_decrypt:将float按照32位二进制数据做异或，然后再转为新的float，
    输入参数为像素块(float32类型)和密钥，输出结果为像素块(float32类型)

#### DCT-test2.py
由于在压缩过程中存在很多取整操作，所以会涉及到许多的整数与小数转换，在该文件中，设置了isFirstStage和isSecondStage来控制这些取证操作。
isFirstStage表示发送者在向平台发布的过程会不会取整；
isSecondStage表示图片压缩时的量化过程会不会取整。
DCT-test1.py用异或加密，本文件用随机乘除加密

#### DCT-test3.py
用于测试在不加密状态下isFirstStage带来的影响，证明这是工程问题不是理论问题

#### myJpegCompress.py
代码实现JPEG编码、解码、压缩过程

#### AES-encryption.py
使用AES加密图片，并测试压缩后的效果，此处可以采用标准图片压缩库


后续实验需要找到一个良好的加密算法，可以看看抗压缩的水印技术是怎么实现的