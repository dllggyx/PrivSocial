import base64
import math
import random
import struct

import numpy as np
import cv2
from myJpegCompress import compress, decompress
from configparser import ConfigParser

# 初始化解析器
config = ConfigParser()
# 读取配置文件
config.read('config.ini',encoding='utf-8')

# class Config:
#     def __init__(self):
#         self.quantizationFlag = True
#         self.downsamplingFlag = True

def alpha(u):
    if u == 0:
        return 1 / np.sqrt(8)
    else:
        return 1 / 2


def block_fill(block):
    block_size = 8
    dst = np.zeros((block_size, block_size), dtype=np.uint8)
    h, w = block.shape
    dst[:h, :w] = block
    return dst


def DCT_block(img):
    block_size = 8
    img = block_fill(img)
    img_fp32 = img.astype(np.float32)
    img_fp32 -= 128
    img_dct = np.zeros((block_size, block_size), dtype=np.float32)
    for line in range(block_size):
        for row in range(block_size):
            n = 0
            for x in range(block_size):
                for y in range(block_size):
                    n += img_fp32[x, y] * math.cos(line * np.pi * (2 * x + 1) / 16) * math.cos(
                        row * np.pi * (2 * y + 1) / 16)
            img_dct[line, row] = alpha(line) * alpha(row) * n
    return np.ceil(img_dct)


def DCT(image):
    block_size = 8
    h, w = image.shape
    dlist = []
    for i in range((h + block_size - 1) // block_size):
        for j in range((w + block_size - 1) // block_size):
            img_block = image[i * block_size:(i + 1) * block_size, j * block_size:(j + 1) * block_size]
            # 处理一个像素块
            img_dct = DCT_block(img_block)
            dlist.append(img_dct)
    return dlist


'''
将加密方式由异或变为乘除
'''
def block_float32_encrypt_decrypt(image_block, random, isEncrypt):
    # config.read('../config.ini',encoding='utf-8')
    keyRangeFloor = config.getfloat('GlobalVars', 'keyRangeFloor')
    keyRangeCeil = config.getfloat('GlobalVars', 'keyRangeCeil')

    (w, h) = image_block.shape
    result_block = np.zeros(image_block.shape, dtype=np.float32)
    # key = 0x0000000000000187
    # key = random.randrange(0, (1 << 10))
    for i in range(0, w):
        for j in range(0, h):
            '''
            取float整数部分出来，存为int型，只加密int低14位，超过16320按照16320计算
            '''
            '''the absolute value of result_block must < 16320'''

            # 因为方块做了旋转和翻转，所以要找准[0][0]实际在哪个位置
            # 这几个if作用是保证DC系数未被加密
            # if i == 0 and j == 0:
            #     result_block[i][j] = image_block[i][j]
            #     continue
            # if i == w-1 and j == 0:
            #     result_block[i][j] = image_block[i][j]
            #     continue
            # if i == 0 and j == h-1:
            #     result_block[i][j] = image_block[i][j]
            #     continue
            # if i == w-1 and j == h-1:
            #     result_block[i][j] = image_block[i][j]
            #     continue



            integer = int(image_block[i][j])
            decimal = image_block[i][j] - integer
            # #提取负号
            # isMinus = False
            # if integer < 0:
            #     integer = -integer
            #     isMinus = True

            key = random.uniform(keyRangeFloor, keyRangeCeil)
            # key = random.uniform(0.1, 1)
            # print(key)

            if isEncrypt:
                # if operator == 0:
                result_block[i][j] = integer * key
                # else:
                #     result_block[i][j] = image_block[i][j] / key
            else:
                # if operator == 0:
                result_block[i][j] = integer / key
                # else:
                #     result_block[i][j] = image_block[i][j] * key


            # if integer > 1024:
            #     # print(integer)
            #     integer = 1024
            # if integer < -1024:
            #     # print(integer)
            #     integer = -1024
    return result_block


# 分块DCT变换(8*8)
# 输入：uint8的灰度图；输出：float32的灰度图
def dct_block(image, key_seed):
    random.seed(key_seed)
    (w, h) = image.shape
    image = image.astype(np.float32)
    image = image - 128
    result_image = image
    for j in range(0, int(w / 8)):
        for i in range(0, int(h / 8)):
            rect = image[8 * i:8 * i + 8, 8 * j:8 * j + 8]
            rect_dct = cv2.dct(rect)
            #加密操作start
            en_rect_dct = block_float32_encrypt_decrypt(rect_dct, random, True)
            # en_rect_dct = rect_dct
            rect_idct = cv2.idct(en_rect_dct)
            # 加密操作end
            result_image[8 * i:8 * i + 8, 8 * j:8 * j + 8] = rect_idct
    return result_image


# 分块逆DCT变换(8*8)
# 输入：float32的灰度图；输出：float32的灰度图
'''
图片压缩后，二进制文件被重写，如何读取该二进制文件，使得能重现dct变换
'''
def idct_block(image, key_seed, quantizationFlag):
    compressQuality = config.getint('GlobalVars', 'compressQuality')
    decompressQuality = config.getint('GlobalVars', 'decompressQuality')
    random.seed(key_seed)
    (w, h) = image.shape
    image = image.astype(np.float32)
    result_image = image
    for j in range(0, int(w / 8)):
        for i in range(0, int(h / 8)):
            rect = image[8 * i:8 * i + 8, 8 * j:8 * j + 8]
            rect_idct = cv2.dct(rect)
            if quantizationFlag:
                rect_idct = compress_emulate(rect_idct, True, compressQuality)
                # 解密操作start
                rect_idct = compress_emulate(rect_idct, False, decompressQuality)
            de_rect_idct = block_float32_encrypt_decrypt(rect_idct, random, False)
            # de_rect_idct = rect_idct
            # 解密操作end
            rect_idct = cv2.idct(de_rect_idct)
            rect_idct = rect_idct + 128
            result_image[8 * i:8 * i + 8, 8 * j:8 * j + 8] = rect_idct
    return result_image


def idct_block_decompress(image, key_seed):
    decompressQuality = config.getint('GlobalVars', 'decompressQuality')
    random.seed(key_seed)
    (w, h) = image.shape
    image = image.astype(np.float32)
    result_image = image
    for j in range(0, int(w / 8)):
        for i in range(0, int(h / 8)):
            rect = image[8 * i:8 * i + 8, 8 * j:8 * j + 8]
            ''' 如果要模拟压缩的话就取消注释这行，并将下一行参数中的rect变为rect_idct '''
            # rect_idct = compress_emulate(rect, False, decompressQuality)
            de_rect_idct = block_float32_encrypt_decrypt(rect, random, False)
            rect_idct = cv2.idct(de_rect_idct)
            rect_idct = rect_idct + 128
            result_image[8 * i:8 * i + 8, 8 * j:8 * j + 8] = rect_idct
    return result_image


def idct_block_pure(image):
    (w, h) = image.shape
    image = image.astype(np.float32)
    result_image = image
    for j in range(0, int(w / 8)):
        for i in range(0, int(h / 8)):
            rect = image[8 * i:8 * i + 8, 8 * j:8 * j + 8]
            rect_idct = cv2.idct(rect)
            # rect_idct = rect_idct + 128
            result_image[8 * i:8 * i + 8, 8 * j:8 * j + 8] = rect_idct
    return result_image


def compress_before_idct(image):
    compressQuality = config.getint('GlobalVars', 'compressQuality')
    (w, h) = image.shape
    image = image.astype(np.float32)
    result_image = image
    for j in range(0, int(w / 8)):
        for i in range(0, int(h / 8)):
            rect = image[8 * i:8 * i + 8, 8 * j:8 * j + 8]
            rect_dct = cv2.dct(rect)
            ''' 如果要模拟压缩就取消注释这行，并将下一行的rect_dct改为rect_idct '''
            # rect_idct = compress_emulate(rect_dct, True, compressQuality)
            result_image[8 * i:8 * i + 8, 8 * j:8 * j + 8] = rect_dct
    return result_image


'''
该函数模拟了量化过程，img_block为经过dct处理的8*8像素块，类型为float32
'''
def compress_emulate(img_block, isCompress, quality_scale=50):

    Qy = np.array([[16, 11, 10, 16, 24, 40, 51, 61],
                   [12, 12, 14, 19, 26, 58, 60, 55],
                   [14, 13, 16, 24, 40, 57, 69, 56],
                   [14, 17, 22, 29, 51, 87, 80, 62],
                   [18, 22, 37, 56, 68, 109, 103, 77],
                   [24, 35, 55, 64, 81, 104, 113, 92],
                   [49, 64, 78, 87, 103, 121, 120, 101],
                   [72, 92, 95, 98, 112, 100, 103, 99]], dtype=np.uint8)
    if quality_scale <= 0:
        quality_scale = 1
    elif quality_scale >= 100:
        quality_scale = 99
    for i in range(64):
        tmp = int((Qy[int(i / 8)][i % 8] * quality_scale + 50) / 100)
        if tmp <= 0:
            tmp = 1
        elif tmp > 255:
            tmp = 255
        Qy[int(i / 8)][i % 8] = tmp

    isSecondStage = True
    if isSecondStage:
        if isCompress:
            img_block[:] = np.round(img_block / Qy)
            # img_block[:] = img_block / Qy
        else:
            img_block[:] = np.round(img_block * Qy)
            # img_block[:] = img_block * Qy
    else:
        if isCompress:
            # img_block[:] = np.round(img_block / Qy)
            img_block[:] = img_block / Qy
        else:
            # img_block[:] = np.round(img_block * Qy)
            img_block[:] = img_block * Qy
    return img_block

def downsampling(img_block):
    # 深拷贝一张图
    dst_block = img_block.copy()
    pos_i = 0
    while pos_i < 8:
        pos_j = 0
        while pos_j < 8:
            value = dst_block[pos_i,pos_j]
            dst_block[pos_i,pos_j + 1] = value
            dst_block[pos_i + 1,pos_j] = value
            dst_block[pos_i + 1,pos_j + 1] = value
            pos_j += 2
        pos_i += 2

    return dst_block


'''
图片dct变换后的值域是多少，加密后要确保值位于这个区间内
'''
if __name__ == '__main__':
    isCompress = True
    isFirstStage = False
    QuantizationFlag = True

    # key = np.random.randint(0, 0xFFFFFFFF, size=[8, 8], dtype=np.uint32)
    if isCompress:
        src = cv2.imread('./square3.jpeg', 0)
        cv2.imshow('src', src)
        img_dct = dct_block(src, 1234567)
        cv2.imshow('dct: encrypt img',img_dct.astype(np.uint8))
        cv2.imwrite('./dct-test1-img/encrypt.jpeg', img_dct)
        if isFirstStage:
            img_dct = cv2.imread('./dct-test1-img/encrypt.jpeg',-1)
        # print(img_dct.dtype)
        img_idct = idct_block(img_dct,1234567, QuantizationFlag)
        cv2.imshow('idct: decrypt img', img_idct.astype(np.uint8))
        # cv2.imwrite('./dct-test1-img/decrypt.jpeg', img_idct.astype(np.uint8))
    else:
        src = cv2.imread('./dct-test1-img/encrypt.jpeg', 0)
        img_etc = compress(src, 50)

        img_compress_path = './dct-test1-img/encrpyt-then-compress.jpeg'
        with open(img_compress_path, 'wb') as f:
            f.write(base64.b16decode(img_etc.upper()))

        src = cv2.imread('./dct-test1-img/encrpyt-then-compress.jpeg', 0)
        cv2.imshow('src', src.astype(np.uint8))
        dst = idct_block(src, 1234567, QuantizationFlag)
        cv2.imshow('dst', dst.astype(np.uint8))
    cv2.waitKey(0)



    # 需要搞清楚一个问题，为什么dct->np.uint32->np.float32->idct的结果会有大量雪花
    # 而dct->np.int32->np.float32->idct结果很好

'''
1.整个流程放Twitter里跑
2.从DCT与加密算法理论角度分析
3.不用图片，用比特串作为输入，来观察算法结果


测试三通道图片抗压缩加密效果
测试三通道图片压缩后的效果
测试AES图片加密效果
学习基于DCT的图片水印技术
'''