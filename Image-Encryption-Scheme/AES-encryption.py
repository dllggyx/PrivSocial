'''
AES图片加解密测试产生的文件都会存在./aes-test文件夹下
'''
import base64
import os
import struct

from Crypto.Cipher import AES
import cv2

SINGLE_CHANNEL = 0
RGB_TRIPLE_CHANNEL = 1
YUV_TRIPLE_CHANNEL = 2
B = Y = 1
G = U = 2
R = V = 4

# Mat与二进制文件互转
def mat_to_bin(mat_src, path, mode):
    (width, height) = (0, 0)
    if mode == 0:
        width, height = mat_src.shape
    else:
        width, height, temp = mat_src.shape
    binary_datas = []
    if mode == 0:
        for w in range(0, width):
            for h in range(0, height):
                # bin_dst += bin(mat_src[w][h]).lstrip("0b").zfill(8)
                binary_data = struct.pack('B', mat_src[w][h])
                binary_datas.append(binary_data)
    elif mode == 1:
        b, g, r = cv2.split(mat_src)
        for w in range(0, width):
            for h in range(0, height):
                binary_datas.append(struct.pack('B', b[w][h]))
                binary_datas.append(struct.pack('B', g[w][h]))
                binary_datas.append(struct.pack('B', r[w][h]))
    elif mode == 2:
        ycbcr_image = cv2.cvtColor(mat_src, cv2.COLOR_BGR2YUV)
        img_Y = ycbcr_image[:, :, 0]
        img_U = ycbcr_image[:, :, 1]
        img_V = ycbcr_image[:, :, 2]
        for w in range(0, width):
            for h in range(0, height):
                binary_datas.append(struct.pack('B', img_Y[w][h]))
                binary_datas.append(struct.pack('B', img_U[w][h]))
                binary_datas.append(struct.pack('B', img_V[w][h]))

    if os.path.exists(path):
        os.remove(path)
    with open(path, 'ab') as file:
        for d in binary_datas:
            file.write(d)


def bin_to_mat(mat_dst_copy, bin_src_path, mode):
    (w, h) = (0, 0)
    if mode == 0:
        w, h = mat_dst_copy.shape
    else:
        w, h, temp = mat_dst_copy.shape
    dst_mat = mat_dst_copy.copy()
    if mode == 2:
        dst_mat = cv2.cvtColor(mat_dst_copy, cv2.COLOR_BGR2YUV)
    w_cnt = 0
    h_cnt = 0
    with open(bin_src_path, 'rb') as file:
        byte = b'00000000'
        while True:
            byte0 = file.read(1)
            # print(byte0)
            byte1 = byte2 = byte0
            if mode != 0:
                byte1 = file.read(1)
                byte2 = file.read(1)
            byte = byte2
            if byte == b'':
                break

            if mode == 0:
                dst_mat[w_cnt][h_cnt] = int.from_bytes(byte0, byteorder='big', signed=False)
                # print(dst_mat[w_cnt][h_cnt])
                h_cnt += 1
                if h_cnt % h == 0:
                    h_cnt = 0
                    w_cnt += 1
            elif mode == 1:
                dst_mat[:, :, 0][w_cnt][h_cnt] = int.from_bytes(byte0, byteorder='big', signed=False)
                dst_mat[:, :, 1][w_cnt][h_cnt] = int.from_bytes(byte1, byteorder='big', signed=False)
                dst_mat[:, :, 2][w_cnt][h_cnt] = int.from_bytes(byte2, byteorder='big', signed=False)
                h_cnt += 1
                if h_cnt % h == 0:
                    h_cnt = 0
                    w_cnt += 1
            elif mode == 2:
                dst_mat[:, :, 0][w_cnt][h_cnt] = int.from_bytes(byte0, byteorder='big', signed=False)
                dst_mat[:, :, 1][w_cnt][h_cnt] = int.from_bytes(byte1, byteorder='big', signed=False)
                dst_mat[:, :, 2][w_cnt][h_cnt] = int.from_bytes(byte2, byteorder='big', signed=False)
                h_cnt += 1
                if h_cnt % h == 0:
                    h_cnt = 0
                    w_cnt += 1
        if mode == 2:
            mat = cv2.cvtColor(dst_mat, cv2.COLOR_YUV2BGR)
            dst_mat = mat
    return dst_mat


def crypt(file_in, file_out, key, kind='e'):
    # key must be 16, 24, 32 bytes long (respectively for *AES-128*, *AES-192* or *AES-256*)
    """kind = e/d, for [e]ncrypt or [d]ecrypt"""
    with open(file_in, 'rb') as f:
        t = f.read()
    aes = AES.new(key.encode(), AES.MODE_ECB)

    if kind == 'e':
        # t += (16 - len(t) % 16) * b'\0'
        func = aes.encrypt
    elif kind == 'd':
        func = aes.decrypt
    else:
        raise ValueError('param: kind should be e or d')
    with open(file_out, 'wb') as o:
        o.write(func(t))


'''
只适用于三通道图片，单通道图片勿用
只加密指定通道
B: 0b001, G: 0b010, R: 0b100
Y: 0b001, U: 0b001, V: 0b001
若加密YU通道，则select = Y | U
各个通道分开加密
'''
def crypt_select(file_in, file_out, key, select, kind='e'):
    select = bin(select).lstrip("0b").zfill(3)
    (byte_listY, byte_listU, byte_listV) = ([], [], [])
    temp_file_path = './aes-test/temp'
    with open(file_in, 'rb') as file:
        byte = b''
        while True:
            byte0 = file.read(1)
            byte1 = file.read(1)
            byte2 = file.read(1)
            byte = byte2
            if byte == b'':
                break
            byte_listY.append(byte0)
            byte_listU.append(byte1)
            byte_listV.append(byte2)
        file.close()

    if select[2] == '1':  # B or Y
        result_listY = process_channel(temp_file_path, byte_listY, 0, key, kind)
        byte_listY = result_listY
    if select[1] == '1':  # G or U
        result_listU = process_channel(temp_file_path, byte_listU, 1, key, kind)
        byte_listU = result_listU
    if select[0] == '1':  # R or V
        result_listV = process_channel(temp_file_path, byte_listV, 2, key, kind)
        byte_listV = result_listV

    # 拼接文件
    if os.path.exists(file_out):
        os.remove(file_out)
    with open(file_out, 'ab') as file:
        length = len(byte_listY)
        for i in range(0, length):
            file.write(byte_listY[i])
            file.write(byte_listU[i])
            file.write(byte_listV[i])

def process_channel(temp_file_path, list, index, key, kind):
    # 先将提取出的指定通道数据保存到文件temp_file_path + str(index)
    result_list = []
    with open(temp_file_path + str(index), 'ab') as f:
        for d in list:
            f.write(d)
        f.close()
    #对文件做加密，存到_crypt中，删除临时文件temp_file_path + str(index)
    crypt(temp_file_path + str(index), temp_file_path + str(index) + '_crypt', key, kind)
    if os.path.exists(temp_file_path + str(index)):
        os.remove(temp_file_path + str(index))
    #提取加密结果，删除临时文件_crypt
    with open(temp_file_path + str(index) + '_crypt', 'rb') as file:
        byte = b''
        while True:
            byte = file.read(1)
            if byte == b'':
                break
            result_list.append(byte)
        file.close()
    if os.path.exists(temp_file_path + str(index) + '_crypt'):
        os.remove(temp_file_path + str(index) + '_crypt')
    return result_list


'''
img_data为待压缩图像
path为压缩后图片存储路径
flag=0返回自解压图像，flag=1返回官方解压图像

返回值为压缩后的图片Mat对象
'''
# def compress_then_save(img_data, path, flag):
#     img_compress = compress(img_data, 50)
#     # 存储压缩后的图像
#     with open(path, 'wb') as f:
#         f.write(base64.b16decode(img_compress.upper()))
#     if flag:
#         img = cv2.imread(path, -1)
#         return img
#     else:
#         img = decompress(path)
#         return img



if __name__ == '__main__':
    # 模式选择
    mode = RGB_TRIPLE_CHANNEL
    # 路径选择，用于存储中间生成的二进制文件
    # path = './aes-test/cipher_encap'
    path = './aes-test/bin_dst'
    # 源图片路径
    idx = 3
    img_path = './result/src/square' + str(idx) + '.jpeg'
    # img_path = './aes-test/encrypt.jpeg'
    # img_path = './aes-test/src.jpeg'
    en_path = './aes-test/bin_dst_en'
    de_path = './aes-test/bin_dst_de'
    key = 'asoh' * 8

    copy = None
    if mode == SINGLE_CHANNEL:
        src = cv2.imread(img_path, 0)
        copy = src * 0
        mat_to_bin(src, path, mode)
    elif mode == RGB_TRIPLE_CHANNEL or mode == YUV_TRIPLE_CHANNEL:
        src = cv2.imread(img_path, -1)
        copy = src * 0
        mat_to_bin(src, path, mode)

    select = R | G | B
    isCompress = False
    decompress_method = 1  # 0自解压，1官方解压
    if isCompress:
        if mode == 0 or select == (R | G | B):
            crypt(path, en_path, key, 'e')
            # mat_en = bin_to_mat(copy, en_path, mode)
            # cv2.imshow('encrypt', mat_en)
            # mat_etc = compress_then_save(mat_en, './aes-test/etc.jpeg', decompress_method)
            # cv2.imshow('encrypt_then_compress', mat_etc)
            # mat_to_bin(mat_etc, path + '_etc', mode)
            # crypt(path + '_etc', de_path, key, 'd')
            # mat_de = bin_to_mat(copy, de_path, mode)
            # cv2.imshow('decrypt', mat_de)
        else:
            path_select_en = en_path + '_select'
            path_select_de = de_path + '_select'
            crypt_select(path, path_select_en, key, select, 'e')
            crypt_select(path_select_en, path_select_de, key, select, 'd')
            mat_en = bin_to_mat(copy, path_select_en, mode)
            cv2.imshow('encrypt', mat_en)
            mat_de = bin_to_mat(copy, path_select_de, mode)
            cv2.imshow('decrypt', mat_de)
    else:
        if mode == 0 or select == (R|G|B):
            ''' crypt(path, en_path, key, 'e') '''

            # eee = cv2.imread('./aes-test/encrypt.png',-1)
            # mat_to_bin(eee, en_path, mode)


            ''' crypt(en_path, de_path, key, 'd') '''
            # crypt('./aes-test/temp', './aes-test/temp_d', key, 'd')
            #
            ddd = bin_to_mat(copy, './aes-test/decode_encap', mode)
            cv2.imwrite('./aes-test/decrypt_encap.jpeg', ddd)
            cv2.imshow('decrypt', ddd)
            cv2.waitKey(0)

            # mat_en = bin_to_mat(copy, en_path, mode)

            ''' 
            mat_en = bin_to_mat(copy, en_path, mode)
            # cv2.imwrite('./aes-test/encrypt.jpeg', mat_en)
            cv2.imwrite('./result/aes_enc/square' + str(idx) + '.jpeg', mat_en)
            # cv2.imshow('encrypt', mat_en)
            mat_de = bin_to_mat(copy, de_path, mode)
            # cv2.imshow('decrypt', mat_de)
            '''
        else:
            path_select_en = en_path + '_select'
            path_select_de = de_path + '_select'
            crypt_select(path, path_select_en, key, select, 'e')
            crypt_select(path_select_en, path_select_de, key, select, 'd')
            mat_en = bin_to_mat(copy, path_select_en, mode)
            cv2.imshow('encrypt', mat_en)
            mat_de = bin_to_mat(copy, path_select_de, mode)
            cv2.imshow('decrypt', mat_de)

    # cv2.waitKey(0)

'''
接下来测试：
一、灰度加密测试
1.AES加密灰度图效果
2.压缩加密图并解密效果
二、RGB单通道加密测试
1.AES加密RGB中某一个通道效果
2.压缩加密图并解密效果
三、YUV加密测试
1.AES加密Y效果
2.压缩加密图并解密效果
3.AES加密U or V效果
4.压缩加密图并解密效果
'''