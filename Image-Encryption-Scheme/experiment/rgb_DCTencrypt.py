import base64
import math

import cv2
import matplotlib.pyplot as plt
import numpy as np
import random
import DCT_test2
import myJpegCompress
from configparser import ConfigParser

# 初始化解析器
config = ConfigParser()
# 读取配置文件
config.read('config.ini',encoding='utf-8')


isDecrypt = config.getboolean('GlobalVars', 'isDecrypt')
isBlock = config.getboolean('GlobalVars', 'isBlock')
isYAntiCompressEncrypt = config.getboolean('GlobalVars', 'isYAntiCompressEncrypt')
isEncryptUV = config.getboolean('GlobalVars', 'isEncryptUV')
isXor = config.getboolean('GlobalVars', 'isXor')
isFirstStage = config.getboolean('GlobalVars', 'isFirstStage')
downSamplingFlag = config.getboolean('GlobalVars', 'downSamplingFlag')
quantizationFlag = config.getboolean('GlobalVars', 'quantizationFlag')

decimalNum = config.getint('GlobalVars', 'decimalNum')
image_name = config.get('GlobalVars', 'image_name')
key = config.getint('GlobalVars', 'key')
keyY = config.getint('GlobalVars', 'keyY')
M = config.getint('GlobalVars', 'M')
N = config.getint('GlobalVars', 'N')
imgPath = '../result/src/' + image_name

'''
首先将RGB图片分为Y U V三通道
对U V做块加密
对Y做抗压缩加密
看看效果

'''

'''
参数详解：
isDecrypt == False为加密; isDecrypt == True为解密
isBlock == False为对Y通道做抗压缩加密
isYAntiCompressEncrypt == True为对Y通道做块加密
isBlock和isYAntiCompressEncrypt可均为True
isEncryptUV表示是否加密UV通道
isFirstStage = False为不会将图片中小数强转为整数，反之则会
在对Y通道做抗压缩加密的基础上, isXor == False为对Y通道做乘法加密; isXor == True为对Y通道做异或加密
'''
# isDecrypt = True
# # Y是否要块加密
# isBlock = True
# # Y是否抗压缩加密
# isYAntiCompressEncrypt = True
# isEncryptUV = True
# isXor = False
# isFirstStage = False
# # decimalNum控制小数位数，大于20则全部保留
# decimalNum = 100
# image_name = 'square3.jpeg'
# # imgPath = '../img/' + image_name
#
#
# key = 276300238
# keyY = 24580200
#
# # key = 1735916705897
# # keyY = 29340867918
#
# M = 800
# N = 800
#
# # M = 128
# # N = 128
#
# # 控制是否降采样
# downSamplingFlag = True
# quantizationFlag = True


# 使用CV2包得到的不是通常的RGB顺序，而是BGR顺序，在上述代码第3行中可将图片颜色空间由BGR转为RGB
def read_image(path):
    image = cv2.imread(path)  # bgr
    image = image[:, :, ::-1]  # rgb
    return image


def show_image(image):
    plt.imshow(image)
    plt.show()


# 块加密，块解密
def block_encrypt_decrypt(src_Y, src_U, src_V, isBlockDecrypt):
    img_origin_Y = np.copy(src_Y)
    img_origin_U = np.copy(src_U)
    img_origin_V = np.copy(src_V)
    xx,yy = src_Y.shape
    result_Y = np.zeros([xx, yy], np.uint8)
    result_U = np.zeros([xx, yy], np.uint8)
    result_V = np.zeros([xx, yy], np.uint8)

    # ROI裁剪
    ceil_imgU = []
    ceil_imgV = []
    ceil_imgY = []
    for j in range(0, int(N / 8)):
        for i in range(0, int(M / 8)):
            rectU = img_origin_U[8 * i:8 * i + 8, 8 * j:8 * j + 8]
            _rectU = rectU
            if isBlockDecrypt and downSamplingFlag:
                _rectU = DCT_test2.downsampling(rectU)
            ceil_imgU.append(_rectU)
            rectV = img_origin_V[8 * i:8 * i + 8, 8 * j:8 * j + 8]
            _rectV = np.copy(rectV)
            if isBlockDecrypt and downSamplingFlag:
                _rectV = DCT_test2.downsampling(rectV)
            ceil_imgV.append(_rectV)
            if isBlock:
                rectY = np.copy(img_origin_Y[8 * i:8 * i + 8, 8 * j:8 * j + 8])
                ceil_imgY.append(rectY)

    Mblock = int(M / 8)
    Nblock = int(N / 8)

    '''
    需要改顺序
    '''
    if isBlockDecrypt:
        random.seed(key)
        # 置换加密
        # ceil_imgU = random.sample(ceil_imgU, len(ceil_imgU))
        # ceil_imgV = random.sample(ceil_imgV, len(ceil_imgV))
        shuffle_array = [a for a in range(int(xx * yy / 64))]
        shuffle_array = random.sample(shuffle_array, len(shuffle_array))
        _ceil_imgU = np.copy(ceil_imgU)
        _ceil_imgV = np.copy(ceil_imgV)
        _ceil_imgY = np.copy(ceil_imgY)
        t = 0
        _j = 0
        while _j < Mblock:
            _i = 0
            while _i < Nblock:
                _ceil_imgU[shuffle_array[t]] = np.copy(ceil_imgU[t])
                _ceil_imgV[shuffle_array[t]] = np.copy(ceil_imgV[t])
                # 如果对Y做块加密，需要让isBlock == True
                if isBlock:
                    _ceil_imgY[shuffle_array[t]] = np.copy(ceil_imgY[t])
                t = t + 1
                _i += 1
            _j += 1

        ceil_imgU = np.copy(_ceil_imgU)
        ceil_imgV = np.copy(_ceil_imgV)
        ceil_imgY = np.copy(_ceil_imgY)

    # 种子密钥k
    random.seed(key)
    # 异或
    for pixel in range(0, int(M * N / 64)):
        mask = np.ones([8, 8], np.uint8)
        # 对每块做异或
        if random.randint(0, 2) == 0:
            mask = mask * 0b00000000
        else:
            mask = mask * 0b11111111

        ceil_imgU[pixel] = cv2.bitwise_xor(ceil_imgU[pixel], mask)
        ceil_imgV[pixel] = cv2.bitwise_xor(ceil_imgV[pixel], mask)
        if isBlock:
            ceil_imgY[pixel] = cv2.bitwise_xor(ceil_imgY[pixel], mask)
        # 对每块做翻转
        temp_judge = random.randint(0, 4)
        if not isBlockDecrypt:
            if temp_judge == 0:
                ceil_imgU[pixel] = cv2.rotate(ceil_imgU[pixel], cv2.ROTATE_90_CLOCKWISE)
                ceil_imgV[pixel] = cv2.rotate(ceil_imgV[pixel], cv2.ROTATE_90_CLOCKWISE)
                if isBlock:
                    ceil_imgY[pixel] = cv2.rotate(ceil_imgY[pixel], cv2.ROTATE_90_CLOCKWISE)
            elif temp_judge == 1:
                ceil_imgU[pixel] = cv2.rotate(ceil_imgU[pixel], cv2.ROTATE_180)
                ceil_imgV[pixel] = cv2.rotate(ceil_imgV[pixel], cv2.ROTATE_180)
                if isBlock:
                    ceil_imgY[pixel] = cv2.rotate(ceil_imgY[pixel], cv2.ROTATE_180)
            elif temp_judge == 2:
                ceil_imgU[pixel] = cv2.rotate(ceil_imgU[pixel], cv2.ROTATE_90_COUNTERCLOCKWISE)
                ceil_imgV[pixel] = cv2.rotate(ceil_imgV[pixel], cv2.ROTATE_90_COUNTERCLOCKWISE)
                if isBlock:
                    ceil_imgY[pixel] = cv2.rotate(ceil_imgY[pixel], cv2.ROTATE_90_COUNTERCLOCKWISE)
        else:
            if temp_judge == 0:
                ceil_imgU[pixel] = cv2.rotate(ceil_imgU[pixel], cv2.ROTATE_90_COUNTERCLOCKWISE)
                ceil_imgV[pixel] = cv2.rotate(ceil_imgV[pixel], cv2.ROTATE_90_COUNTERCLOCKWISE)
                if isBlock:
                    ceil_imgY[pixel] = cv2.rotate(ceil_imgY[pixel], cv2.ROTATE_90_COUNTERCLOCKWISE)
            elif temp_judge == 1:
                ceil_imgU[pixel] = cv2.rotate(ceil_imgU[pixel], cv2.ROTATE_180)
                ceil_imgV[pixel] = cv2.rotate(ceil_imgV[pixel], cv2.ROTATE_180)
                if isBlock:
                    ceil_imgY[pixel] = cv2.rotate(ceil_imgY[pixel], cv2.ROTATE_180)
            elif temp_judge == 2:
                ceil_imgU[pixel] = cv2.rotate(ceil_imgU[pixel], cv2.ROTATE_90_CLOCKWISE)
                ceil_imgV[pixel] = cv2.rotate(ceil_imgV[pixel], cv2.ROTATE_90_CLOCKWISE)
                if isBlock:
                    ceil_imgY[pixel] = cv2.rotate(ceil_imgY[pixel], cv2.ROTATE_90_CLOCKWISE)

    _ceil_imgU = np.copy(ceil_imgU)
    _ceil_imgV = np.copy(ceil_imgV)
    _ceil_imgY = np.copy(ceil_imgY)


    if not isBlockDecrypt:
        random.seed(key)
        # 置换加密
        # ceil_imgU = random.sample(ceil_imgU, len(ceil_imgU))
        # ceil_imgV = random.sample(ceil_imgV, len(ceil_imgV))
        shuffle_array = [a for a in range(int(xx * yy / 64))]
        shuffle_array = random.sample(shuffle_array, len(shuffle_array))
        t = 0
        _j = 0
        while _j < Mblock:
            _i = 0
            while _i < Nblock:
                _ceil_imgU[t] = np.copy(ceil_imgU[shuffle_array[t]])
                _ceil_imgV[t] = np.copy(ceil_imgV[shuffle_array[t]])
                # 如果对Y做块加密，需要让isBlock == True
                if isBlock:
                    _ceil_imgY[t] = np.copy(ceil_imgY[shuffle_array[t]])
                t = t + 1
                _i += 1
            _j += 1

        t = 0
        _j = 0

    for j in range(0, int(N / 8)):
        for i in range(0, int(M / 8)):
            result_U[8 * i:8 * i + 8, 8 * j:8 * j + 8] = _ceil_imgU[i + j * Nblock]
            result_V[8 * i:8 * i + 8, 8 * j:8 * j + 8] = _ceil_imgV[i + j * Nblock]
            if isBlock:
                result_Y[8 * i:8 * i + 8, 8 * j:8 * j + 8] = _ceil_imgY[i + j * Nblock]
    return result_Y, result_U, result_V


def proposed_scheme_float_to_uint8(src_Y):
    keyRangeCeil = config.getfloat('GlobalVars', 'keyRangeCeil')

    src = np.copy(src_Y)
    # # 先抹除小数位
    # src_integer = np.round(src, decimals=0)
    # dim1, dim2 = src_integer.shape
    # mask = np.ones((dim1, dim2))
    # mask = mask * 255
    # # 获得商和余数
    # quotients, remainders = np.modf(src_integer / mask)
    # # 拼接余数和熵
    #
    # print(src_integer)
    # print(src_integer.max())

    src = src + math.floor(255 * (keyRangeCeil + 0.5)/2)
    src = src / 10
    src_integer = np.round(src, decimals=0)
    src_integer = src_integer.astype(np.uint8)
    return src_integer


def proposed_scheme_uint8_to_float(src_Y):
    keyRangeCeil = config.getfloat('GlobalVars', 'keyRangeCeil')

    src = np.copy(src_Y)
    src = src.astype(np.float32)
    src = src * 10
    src = src - math.floor(255 * (keyRangeCeil + 0.5)/2)
    return src


def main(img_path):
    rgb_image = cv2.imread(img_path)
    # cv2.imshow('src', rgb_image)
    M,N,_ = rgb_image.shape

    # 色彩空间转换(由RGB-->YUV)
    ycbcr_image = cv2.cvtColor(rgb_image, cv2.COLOR_BGR2YUV)
    # cv2.imshow('original image converted to YUV', ycbcr_image)
    # cv2.waitKey(0)
    # img_originalData_Y_before, img_originalData_U, img_originalData_V为未经操作的原始数据
    img_originalData_Y = ycbcr_image[:, :, 0]
    img_originalData_U = ycbcr_image[:, :, 1]
    img_originalData_V = ycbcr_image[:, :, 2]

    # 为cipher赋初值
    ciphertext_Y = np.copy(img_originalData_Y)
    ciphertext_U = np.copy(img_originalData_U)
    ciphertext_V = np.copy(img_originalData_V)

    ciphertext_Y_after = np.copy(ciphertext_Y)

    eY, eU, eV = block_encrypt_decrypt(ciphertext_Y, ciphertext_U, ciphertext_V, False)
    ciphertext_U = np.copy(eU)
    ciphertext_V = np.copy(eV)
    if isBlock:
        ciphertext_Y = np.copy(eY)
        ciphertext_Y_after = np.copy(eY)


    # 如果采用异或加密，没有封装好的函数，需要先经过dct，否则不需要
    img_Y_before = None
    img_Y_after = None
    if isYAntiCompressEncrypt:
        if not isBlock:
            if isXor:
                img_Y_before = img_originalData_Y.astype(np.float32)
                img_Y_after = cv2.dct(img_Y_before)
                img_Y_after = img_originalData_Y.astype(np.uint8)
            else:
                img_Y_after = img_Y_before
        else:
            if isXor:
                img_Y_before = ciphertext_Y.astype(np.float32)
                img_Y_after = cv2.dct(img_Y_before)
                img_Y_after = ciphertext_Y.astype(np.uint8)
            else:
                img_Y_after = img_Y_before


    Key_1 = np.random.randint(0, 255, size=[M, N], dtype=np.uint8)

    if isYAntiCompressEncrypt:
        if isXor:
            ciphertext_Y = cv2.bitwise_xor(img_Y_after, Key_1)
            ciphertext_Y = ciphertext_Y.astype(np.float32)
            ciphertext_Y_after = cv2.idct(ciphertext_Y)
            if isFirstStage:
                ciphertext_Y_after = ciphertext_Y_after.astype(np.uint8)
        else:
            # 加密Y通道
            img_dct_Y = DCT_test2.dct_block(ciphertext_Y, keyY)
            if isFirstStage:
                ciphertext_Y_after = img_dct_Y.astype(np.uint8)
            else:
                if decimalNum > 20:
                    ciphertext_Y_after = img_dct_Y
                else:
                    ciphertext_Y_after = np.round(img_dct_Y, decimals=0)
                    # ciphertext_Y_after = proposed_scheme_float_to_uint8(img_dct_Y)

                    print(ciphertext_Y_after)
                    # img_originalData_Y = img_originalData_Y.astype(np.float32)
                    # img_originalData_Y2 = DCT_test2.dct_block(img_originalData_Y, 12345)
                    # ciphertext_Y_after = proposed_scheme_float_to_uint8(img_originalData_Y2)
                    #
                    # ttttt = DCT_test2.compress_before_idct(ciphertext_Y_after)
                    # ttttt1 = DCT_test2.idct_block_nocompress(ttttt, 12345)
                    # cv2.imshow('ttttt1',ttttt1.astype(np.uint8))
                    # cv2.waitKey(0)


        # cv2.imshow('Y channel\'s result with anti-compress encrypt, isXor = ' + str(isXor), ciphertext_Y_after.astype(np.uint8))
        # cv2.waitKey(0)


    # 显示加密效果
    ciphertext_YUV = cv2.merge([ciphertext_Y_after.astype(np.uint8), ciphertext_U, ciphertext_V])
    encrypt_rgb = cv2.cvtColor(ciphertext_YUV, cv2.COLOR_YUV2BGR)
    cv2.imshow('YUV channels are all encrypted and then converted to RGB', encrypt_rgb)
    # cv2.imwrite('../result/ourscheme_enc/' + image_name, encrypt_rgb)
    #  代码不影响逻辑，仅用于查看效果
    ciphertext_YUV_1 = cv2.merge([ciphertext_Y_after.astype(np.uint8), img_originalData_U, img_originalData_V])
    encrypt_rgb_1 = cv2.cvtColor(ciphertext_YUV_1, cv2.COLOR_YUV2BGR)
    cv2.imshow('only Y channel is encrypted and then converted to RGB', encrypt_rgb_1)
    #  代码不影响逻辑，仅用于查看效果
    ciphertext_YUV_2 = cv2.merge([img_originalData_Y.astype(np.uint8), ciphertext_U, ciphertext_V])
    encrypt_rgb_2 = cv2.cvtColor(ciphertext_YUV_2, cv2.COLOR_YUV2BGR)
    # cv2.imshow('only UV channels are encrypted and then converted to RGB', encrypt_rgb_2)
    # cv2.waitKey(0)
    # if isYAntiCompressEncrypt:
    #     cv2.imwrite('../img/en_' + image_name, encrypt_rgb)
    # else:
    #     cv2.imwrite('../img/en_block_' + image_name, encrypt_rgb)

    # 初始化解密结果
    decryptedtext_Y = ciphertext_Y_after.copy()
    decryptedtext_U = ciphertext_U.copy()
    decryptedtext_V = ciphertext_V.copy()
    if not isEncryptUV:
        origin = cv2.cvtColor(rgb_image, cv2.COLOR_BGR2YUV)
        decryptedtext_U = origin[:, :, 1]
        decryptedtext_V = origin[:, :, 2]

    # result_image = cv2.cvtColor(ycbcr_image, cv2.COLOR_YUV2BGR)
    # cv2.imshow('result',result_image)
    # cv2.waitKey(0)

    if isDecrypt:
        # 解密
        if isYAntiCompressEncrypt:
            if isXor:
                ciphertext_Y_after = ciphertext_Y_after.astype(np.float32)
                ciphertext_Y_after_dct = cv2.dct(ciphertext_Y_after)
                ciphertext_Y_after_dct = ciphertext_Y_after_dct.astype(np.uint8)
                decryptedtext_Y_before = cv2.bitwise_xor(ciphertext_Y_after_dct, Key_1)  # 将密文的B通道与密钥进行异或运算
                decryptedtext_Y_before = decryptedtext_Y_before.astype(np.float32)
                decryptedtext_Y = cv2.idct(decryptedtext_Y_before)
                decryptedtext_Y = decryptedtext_Y.astype(np.uint8)
            else:
                # 压缩
                Y_after_compress = DCT_test2.compress_before_idct(ciphertext_Y_after)
                # Y_after_compress_1 = np.round(Y_after_compress, decimals=0)

                # Y_after_compress_uint = proposed_scheme_uint8_to_float(Y_after_compress)
                # idct解密
                decryptedtext_Y = DCT_test2.idct_block_nocompress(Y_after_compress, keyY)

                # 压缩和idct解密
                # decryptedtext_Y = DCT_test2.idct_block(ciphertext_Y_after, keyY, quantizationFlag)
                decryptedtext_Y = decryptedtext_Y.astype(np.uint8)

        # 块解密
        # 存储压缩后的图像
        u_compress_path = '../img/u.jpeg'
        cv2.imwrite(u_compress_path, ciphertext_U, [cv2.IMWRITE_JPEG_QUALITY, 50])
        u = cv2.imread(u_compress_path, -1)
        ciphertext_U = np.copy(u)

        v_compress_path = '../img/v.jpeg'
        cv2.imwrite(v_compress_path, ciphertext_V, [cv2.IMWRITE_JPEG_QUALITY, 50])
        v = cv2.imread(v_compress_path, -1)
        ciphertext_V = np.copy(v)

        if not isYAntiCompressEncrypt:
            yy_result = myJpegCompress.compress_block(decryptedtext_Y, 50).astype(np.uint8)
            decryptedtext_Y = np.copy(yy_result)
            # cv2.imwrite('../img/y.jpeg',decryptedtext_Y)
            # yy_result = cv2.imread('../img/y.jpeg')

        dY, dU, dV = block_encrypt_decrypt(decryptedtext_Y, ciphertext_U, ciphertext_V, True)
        decryptedtext_U = np.copy(dU)
        decryptedtext_V = np.copy(dV)
        decryptedtext_Y = np.copy(dY)

        decryptedtext_YUV = cv2.merge([decryptedtext_Y, decryptedtext_U, decryptedtext_V])  # 合并通道
        result_decrypt = cv2.cvtColor(decryptedtext_YUV, cv2.COLOR_YUV2BGR)
        cv2.imshow('decrypt Y', decryptedtext_Y)
        # cv2.imshow('decrypt U', decryptedtext_U)
        # cv2.imshow('decrypt V', decryptedtext_V)

        cv2.imshow('YUV are decrypted and converted to RGB', result_decrypt)  # 显示已解密的图片
        cv2.waitKey(0)

        # cv2.imwrite('../img/de_' + image_name, result_decrypt)
        # cv2.imwrite('../img/Q_test/de_40' + image_name, result_decrypt)
        # cv2.imwrite('../img/de_50' + image_name, result_decrypt)
        # cv2.imwrite('../result/ourscheme/' + image_name, result_decrypt)


if __name__ == '__main__':
    main(imgPath)
