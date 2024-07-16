import math
import random
import struct

import numpy as np
import cv2

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


def block_binary_2_float32(image_block):
    (w, h) = image_block.shape
    result_block = np.zeros(image_block.shape, dtype=np.float32)
    for i in range(0, w):
        for j in range(0, h):
            result_block[i][j] = struct.unpack('f', image_block[i][j])[0]
    return result_block


def block_float32_2_binary(image_block, key=None):
    (w, h) = image_block.shape
    result_block = np.zeros(image_block.shape, dtype=np.uint32)
    for i in range(0, w):
        for j in range(0, h):
            '''
            取float整数部分出来，存为int型，只加密int低14位，超过16320按照16320计算
            '''
            binary = struct.pack('!f', image_block[i][j])
            result_block[i][j] = struct.unpack('!I', binary)[0]

            if key is not None:
                '''the absolute value of result_block must < 16320'''
                ''''''
                result_block[i][j] = result_block[i][j] ^ key
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
            # rect_dct = rect_dct * 10
            key = random.randrange(0, (1<<13))
            rect_dct_binary = block_float32_2_binary(rect_dct, key)
            rect_dct = block_binary_2_float32(rect_dct_binary)
            # 加密操作end
            result_image[8 * i:8 * i + 8, 8 * j:8 * j + 8] = rect_dct
    return result_image

# 分块逆DCT变换(8*8)
# 输入：float32的灰度图；输出：float32的灰度图
def idct_block(image, key_seed):
    random.seed(key_seed)

    (w, h) = image.shape
    # image = image.astype(np.float32)
    result_image = image
    for j in range(0, int(w / 8)):
        for i in range(0, int(h / 8)):
            rect = image[8 * i:8 * i + 8, 8 * j:8 * j + 8]
            rect_idct_1 = cv2.idct(rect)
            rect_idct = cv2.dct(rect_idct_1)
            # 解密操作start
            # rect_idct = rect_idct / 10
            key = random.randrange(0, (1<<13))
            rect_idct_binary = block_float32_2_binary(rect_idct, key)
            rect_idct = block_binary_2_float32(rect_idct_binary)
            # 解密操作end
            rect_idct = cv2.idct(rect_idct)
            rect_idct = rect_idct + 128


            result_image[8 * i:8 * i + 8, 8 * j:8 * j + 8] = rect_idct
    return result_image


def block_float32_encrypt_decrypt(image_block, random):
    (w, h) = image_block.shape
    result_block = np.zeros(image_block.shape, dtype=np.float32)
    for i in range(0, w):
        for j in range(0, h):
            '''
            取float整数部分出来，存为int型，只加密int低14位，超过16320按照16320计算
            '''
            '''the absolute value of result_block must < 16320'''
            key = random.randrange(0,(1<<13))
            integer = int(image_block[i][j])
            decimal = image_block[i][j] - integer
            if key is not None:
                integer = integer ^ key
                if integer > 16320:
                    integer = 16320
                if integer < -16320:
                    integer = -16320
            temp = integer + decimal
            binary = struct.pack('!f', temp)
            result_block[i][j] = temp
    return result_block



# def experiment1():
#     Key_1 = np.random.randint(0, 0x66666666, size=[800, 800], dtype=np.uint32)
#
#     # img_dct = DCT(image_y)
#     # 以灰度图打开
#     src = cv2.imread('./square3.jpeg', 0)
#     src_float = src.astype(np.float32)
#     cv2.imshow('src', src)
#
#     dct_img = cv2.dct(src_float)
#     print(dct_img.dtype)
#     dct_img = dct_img.astype(np.int32)
#     # 加密
#     dct_img = cv2.bitwise_xor(dct_img, Key_1)
#     dct_img = dct_img.astype(np.float32)
#     idct_img = cv2.idct(dct_img)
#     cv2.imshow('result1', idct_img.astype(np.uint8))
#
#     dct_img_1 = cv2.dct(idct_img)
#     dct_img_1 = dct_img_1.astype(np.int32)
#     # 解密
#     dct_img_1 = cv2.bitwise_xor(dct_img_1, Key_1)
#     dct_img_1 = dct_img_1.astype(np.float32)
#     idct_img_1 = cv2.idct(dct_img_1)
#     cv2.imshow('result2', idct_img_1.astype(np.uint8))
#     cv2.waitKey(0)

# def experiment2():
#     Key_1 = np.random.randint(0, 0x66666666, size=[800, 800], dtype=np.uint32)
#
#     # img_dct = DCT(image_y)
#     # 以灰度图打开
#     src = cv2.imread('./square3.jpeg', 0)
#     src_float = uint8_2_float32(src)
#     cv2.imshow('src', src)
#
#     dct_img = cv2.dct(src_float)
#     print(dct_img.dtype)
#     dct_img = float32_2_uint8(dct_img)
#     dct_img = uint8_2_float32(dct_img)
#     idct_img = cv2.idct(dct_img)
#     cv2.imshow('result1', float32_2_uint8(idct_img))
#
#
#     dct_img_1 = cv2.dct(idct_img)
#     idct_img_1 = cv2.idct(dct_img_1)
#     cv2.imshow('result2', float32_2_uint8(idct_img_1))
#     cv2.waitKey(0)

# def experiment3():
#     Key_1 = np.random.randint(0, 0x66666666, size=[800, 800], dtype=np.uint32)
#
#     # img_dct = DCT(image_y)
#     # 以灰度图打开
#     src = cv2.imread('./square3.jpeg', 0)
#     src_float = src.astype(np.float32)
#     cv2.imshow('src', src)
#
#     dct_img = cv2.dct(src_float)
#     print(dct_img.dtype)
#     # 加密
#     dct_img = dct_img / 55
#     idct_img = cv2.idct(dct_img)
#     cv2.imshow('result1', idct_img.astype(np.uint8))
#
#     dct_img_1 = cv2.dct(idct_img)
#     # 解密
#     dct_img_1 = dct_img_1 * 55
#     idct_img_1 = cv2.idct(dct_img_1)
#     cv2.imshow('result2', idct_img_1.astype(np.uint8))
#     cv2.waitKey(0)

'''
图片dct变换后的值域是多少，加密后要确保值位于这个区间内
'''
if __name__ == '__main__':
    # key = np.random.randint(0, 0xFFFFFFFF, size=[8, 8], dtype=np.uint32)
    src = cv2.imread('./square3.jpeg', 0)
    cv2.imshow('src', src)
    img_dct = dct_block(src,1234567)
    cv2.imshow('dct',img_dct.astype(np.uint8))
    img_idct = idct_block(img_dct,1234567)
    cv2.imshow('idct', img_idct.astype(np.uint8))
    cv2.waitKey(0)
    # experiment3()


    # 需要搞清楚一个问题，为什么dct->np.uint32->np.float32->idct的结果会有大量雪花
    # 而dct->np.int32->np.float32->idct结果很好
