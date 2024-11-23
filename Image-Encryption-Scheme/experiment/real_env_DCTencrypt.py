import math

import cv2
import matplotlib.pyplot as plt
import numpy as np
import random
import DCT_test2
from PIL import Image
import myJpegCompress
from configparser import ConfigParser
from construct_jpeg.build_jpeg import generate_jpeg, degenerate_jpeg

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


def down_sampling(src_UV):
    img_origin = np.copy(src_UV)
    # ROI裁剪
    result = np.copy(img_origin)

    for j in range(0, int(N / 2)):
        for i in range(0, int(M / 2)):
            block = img_origin[2 * i:2 * i + 2, 2 * j:2 * j + 2]
            # value = block[0, 0] + block[0, 1] + block[1, 0] + block[1, 1]
            # value_int = int(value/4)
            block.fill(block[0, 0])
            result[2 * i:2 * i + 2, 2 * j:2 * j + 2] = block

    return result


# opencv to PIL
def cv2PIL_RGB(img_cv):
    img_rgb = img_cv[:, :, ::-1]  # OpenCV 的通道顺序为 BGR, 转换成RGB
    # nparray
    img_pil = Image.fromarray(np.uint8(img_rgb))
    return img_pil


def main(img_path):
    rgb_image = cv2.imread(img_path)
    # cv2.imshow('src', rgb_image)
    M,N,_ = rgb_image.shape
    cv2.imshow('src', rgb_image)

    # 色彩空间转换(由RGB-->YUV)
    ycbcr_image = cv2.cvtColor(rgb_image, cv2.COLOR_BGR2YUV)

    img_originalData_Y = ycbcr_image[:, :, 0]
    img_originalData_U = ycbcr_image[:, :, 1]
    img_originalData_V = ycbcr_image[:, :, 2]

    # 为cipher赋初值
    ciphertext_Y = np.copy(img_originalData_Y)
    ciphertext_U = np.copy(img_originalData_U)
    ciphertext_V = np.copy(img_originalData_V)

    # 对UV提前做降采样
    dec_U = down_sampling(ciphertext_U)
    dec_V = down_sampling(ciphertext_V)

    eY, eU, eV = block_encrypt_decrypt(ciphertext_Y, dec_U, dec_V, False)
    ciphertext_U = np.copy(eU)
    ciphertext_V = np.copy(eV)
    ciphertext_Y = np.copy(eY)


    # 加密Y通道
    img_dct_Y = DCT_test2.dct_block(ciphertext_Y, keyY)
    ciphertext_Y_after = np.round(img_dct_Y, decimals=0)
    ciphertext_Y_after += 128
    print(np.min(ciphertext_Y_after))
    print(np.max(ciphertext_Y_after))
    print(ciphertext_Y_after.shape)
    ciphertext_Y_after = ciphertext_Y_after.astype(np.uint8)

    # 显示加密效果
    ciphertext_YUV = cv2.merge([ciphertext_Y_after, ciphertext_U, ciphertext_V])
    encrypt_rgb = cv2.cvtColor(ciphertext_YUV, cv2.COLOR_YUV2BGR)
    pil_img = cv2PIL_RGB(encrypt_rgb)
    pil_img.save('construct_jpeg/encode_img/y_' + image_name, quality=100, subsampling=0)
    # cv2.imshow('YUV channels are all encrypted and then converted to RGB', encrypt_rgb)
    # cv2.imwrite('construct_jpeg/encode_img/y_' + image_name, encrypt_rgb, [int( cv2.IMWRITE_JPEG_QUALITY), 100])

    # aa, bb, cc = block_encrypt_decrypt(ciphertext_Y_after, ciphertext_U, ciphertext_V, True)
    #
    # cv2.imshow('Y', aa)
    # cv2.imshow('U', bb)
    # cv2.imshow('V', cc)
    # cv2.waitKey(0)

    '''
    ============================================== Decrypt ==============================================
    '''
    # 初始化解密结果
    # img_dec = cv2.imread('construct_jpeg/encode_img/y_' + image_name)
    img_dec = cv2.imread('construct_jpeg/encode_img/y_square3_aftertwitter.jpg')

    ycbcr_dec = cv2.cvtColor(img_dec, cv2.COLOR_BGR2YUV)
    dec_Y = ycbcr_dec[:, :, 0]
    dec_U = ycbcr_dec[:, :, 1]
    dec_V = ycbcr_dec[:, :, 2]

    if isDecrypt:

        dec_Y = dec_Y.astype(np.float32)
        dec_Y -= 128
        # idct解密
        Y_after_compress = DCT_test2.compress_before_idct(dec_Y)
        decompress_Y = DCT_test2.idct_block_decompress(Y_after_compress, keyY)
        decompress_Y = decompress_Y.astype(np.uint8)
        dec_Y_temp = np.copy(decompress_Y)

        # 块解密
        dY, dU, dV = block_encrypt_decrypt(dec_Y_temp, dec_U, dec_V, True)
        decryptedtext_U = np.copy(dU)
        decryptedtext_V = np.copy(dV)
        decryptedtext_Y = np.copy(dY)

        cv2.imshow('111',decryptedtext_U)
        # 泛华
        # decryptedtext_U1 = np.copy(decryptedtext_U)
        # for j in range(0, int(N / 8)):
        #     for i in range(0, int(M / 8)):
        #         block = decryptedtext_U[8 * i:8 * i + 8, 8 * j:8 * j + 8]
        #         for line in range(0, 8):
        #             block[line, 7] = block[line, 5]
        #             block[7, line] = block[5, line]
        #             block[7 - line, 7] = block[7 - line, 5]
        #             block[7, 7 - line] = block[5, 7 - line]
        #
        #             block[line, 6] = block[line, 5]
        #             block[6, line] = block[5, line]
        #             block[6 - line, 7] = block[7 - line, 5]
        #             block[6, 7 - line] = block[5, 7 - line]
        #         decryptedtext_U1[8 * i:8 * i + 8, 8 * j:8 * j + 8] = block
        #
        # decryptedtext_V1 = np.copy(decryptedtext_V)
        # for j in range(0, int(N / 8)):
        #     for i in range(0, int(M / 8)):
        #         block = decryptedtext_V[8 * i:8 * i + 8, 8 * j:8 * j + 8]
        #         for line in range(0, 8):
        #             block[line, 7] = block[line, 5]
        #             block[7, line] = block[5, line]
        #             block[7 - line, 7] = block[7 - line, 5]
        #             block[7, 7 - line] = block[5, 7 - line]
        #
        #             block[line, 6] = block[line, 5]
        #             block[6, line] = block[5, line]
        #             block[6 - line, 7] = block[7 - line, 5]
        #             block[6, 7 - line] = block[5, 7 - line]
        #         decryptedtext_V1[8 * i:8 * i + 8, 8 * j:8 * j + 8] = block

        decryptedtext_YUV = cv2.merge([decryptedtext_Y, decryptedtext_U, decryptedtext_V])  # 合并通道
        result_decrypt = cv2.cvtColor(decryptedtext_YUV, cv2.COLOR_YUV2BGR)
        cv2.imshow('decrypt Y', decryptedtext_Y)
        cv2.imshow('decrypt U', decryptedtext_U)
        cv2.imshow('decrypt V', decryptedtext_V)
        cv2.imshow('YUV are decrypted and converted to RGB', result_decrypt)  # 显示已解密的图片
        cv2.imwrite('construct_jpeg/encode_img/square3_dec_aftertwitter.jpeg', result_decrypt)
        cv2.waitKey(0)



if __name__ == '__main__':
    main(imgPath)
