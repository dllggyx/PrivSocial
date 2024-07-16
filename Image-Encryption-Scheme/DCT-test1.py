import base64
import math
import random
import struct

import numpy as np
import cv2
from myJpegCompress import compress, decompress

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


def block_float32_encrypt_decrypt(image_block, random):
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
            key = random.randrange(0, (1 << 10))
            integer = int(image_block[i][j])
            decimal = image_block[i][j] - integer
            # #提取负号
            # isMinus = False
            # if integer < 0:
            #     integer = -integer
            #     isMinus = True

            r = random.randrange(0, 2)
            if r == 0:
                key = 0x00000000000003FF
            else:
                key = 0
            if integer != 0:
                integer = integer ^ key
            # if isMinus:
            #     integer = -integer

            if integer > 1024:
                # print(integer)
                integer = 1024
            if integer < -1024:
                # print(integer)
                integer = -1024
            temp = integer + decimal
            # binary = struct.pack('!f', temp)
            result_block[i][j] = temp
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
            en_rect_dct = block_float32_encrypt_decrypt(rect_dct, random)
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
def idct_block(image, key_seed):
    random.seed(key_seed)
    (w, h) = image.shape
    image = image.astype(np.float32)
    result_image = image
    for j in range(0, int(w / 8)):
        for i in range(0, int(h / 8)):
            rect = image[8 * i:8 * i + 8, 8 * j:8 * j + 8]
            rect_idct = cv2.dct(rect)
            rect_idct = compress_emulate(rect_idct, True, 50)
            # 解密操作start
            rect_idct = compress_emulate(rect_idct, False, 50)
            de_rect_idct = block_float32_encrypt_decrypt(rect_idct, random)
            # de_rect_idct = rect_idct
            # 解密操作end
            rect_idct = cv2.idct(de_rect_idct)
            rect_idct = rect_idct + 128
            result_image[8 * i:8 * i + 8, 8 * j:8 * j + 8] = rect_idct
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

    if isCompress:
        img_block[:] = np.round(img_block / Qy)
        # img_block[:] = img_block / Qy
    else:
        img_block[:] = np.round(img_block * Qy)
        # img_block[:] = img_block * Qy
    return img_block

'''
图片dct变换后的值域是多少，加密后要确保值位于这个区间内
'''
if __name__ == '__main__':
    isCompress = True
    # key = np.random.randint(0, 0xFFFFFFFF, size=[8, 8], dtype=np.uint32)
    if isCompress:
        src = cv2.imread('./square3.jpeg', 0)
        cv2.imshow('src', src)
        img_dct = dct_block(src,1234567)
        cv2.imshow('dct: encrypt img',img_dct.astype(np.uint8))
        cv2.imwrite('./dct-test1-img/encrypt.jpeg',img_dct.astype(np.uint8))
        img_idct = idct_block(img_dct,1234567)
        cv2.imshow('idct: decrypt img', img_idct.astype(np.uint8))
        cv2.imwrite('./dct-test1-img/decrypt.jpeg', img_idct.astype(np.uint8))
    else:
        src = cv2.imread('./dct-test1-img/encrypt.jpeg', 0)
        img_etc = compress(src, 50)

        img_compress_path = './dct-test1-img/encrpyt-then-compress.jpeg'
        with open(img_compress_path, 'wb') as f:
            f.write(base64.b16decode(img_etc.upper()))

        src = cv2.imread('./dct-test1-img/encrpyt-then-compress.jpeg', 0)
        cv2.imshow('src', src.astype(np.uint8))
        dst = idct_block(src, 1234567)
        cv2.imshow('dst', dst.astype(np.uint8))
    cv2.waitKey(0)



    # 需要搞清楚一个问题，为什么dct->np.uint32->np.float32->idct的结果会有大量雪花
    # 而dct->np.int32->np.float32->idct结果很好
