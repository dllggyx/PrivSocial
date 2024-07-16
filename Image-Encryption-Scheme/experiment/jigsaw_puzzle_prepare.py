import cv2
import matplotlib.pyplot as plt
import numpy as np

'''
0-10        =>  0
11-20       =>  1
21-30       =>  2
...
241-255     =>  24

'''

def average_pixel(block):
    value = 0
    for i in range(0, 8):
        for j in range(0, 8):
            value += block[i, j]
    value = value / 64
    if int(value) == 0:
        idx = 0
    else:
        idx = int((int(value) - 1) / 10)
    if idx == 25:
        idx = idx - 1
    return idx


# 分块分通道读图片
def dct_block(image, group):
    (w, h) = image.shape
    for j in range(0, int(w / 8)):
        for i in range(0, int(h / 8)):
            rect = image[8 * i:8 * i + 8, 8 * j:8 * j + 8]
            # caculate average pixel value
            idx = average_pixel(rect)
            group[idx] += 1


if __name__ == '__main__':
    idx = 8
    imgPath = '../result/block_enc/square' + str(idx) + '.jpeg'
    src_img = cv2.imread(imgPath)

    B = src_img[:, :, 0]
    G = src_img[:, :, 1]
    R = src_img[:, :, 2]

    yuv_image = cv2.cvtColor(src_img, cv2.COLOR_BGR2YUV)
    Y = yuv_image[:, :, 0]
    U = yuv_image[:, :, 1]
    V = yuv_image[:, :, 2]

    group = [0] * 25
    dct_block(Y, group)
    print(group)
    combine = [0] * 13
    max = 0
    pos = 0
    for i in range(0, 13):
        if i == 12:
            combine[i] = group[i]
        else:
            combine[i] = group[i] + group[24 - i]
        if combine[i] > max:
            max = combine[i]
            pos = i
    print(combine)
    print('=========================')

    # group = [0] * 25
    # dct_block(U, group)
    # print(group)
    # combine = [0] * 13
    # for i in range(0, 13):
    #     if i == 12:
    #         combine[i] = group[i]
    #     else:
    #         combine[i] = group[i] + group[24 - i]
    # print(combine)
    # print('=========================')
    #
    # group = [0] * 25
    # dct_block(V, group)
    # print(group)
    # for i in range(0, 13):
    #     if i == 12:
    #         combine[i] = group[i]
    #     else:
    #         combine[i] = group[i] + group[24 - i]
    # print(combine)
    # print('=========================')

    cv2.imshow('Y', Y)
    cv2.waitKey(0)

# 计算每块的平均像素，然后放到对应的组中


