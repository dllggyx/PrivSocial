import base64
import matplotlib.pyplot as plt  # plt 用于显示图片
import numpy as np
import cv2


def sender(img):
    file_preporcess = ''
    return file_preporcess


def channel(img):
    file = ''
    return file


def recipient(img):
    return img


def dct_maintain_encrypt(img_path):
    img_src = cv2.imread(img_path, 0)

    '''
    发送方预处理与加密
    '''
    preprocess_file = sender(img_src)
    img_to_channel = preprocess_file

    '''
    信道压缩
    '''
    img_download = channel(img_to_channel)

    '''
    接收方解密
    '''
    img_result = recipient(img_download)

    return img_result


if __name__ == '__main__':
    img_path = ''
    dct_maintain_encrypt(img_path)