import cv2
import matplotlib.pyplot as plt
import numpy as np
import random

isDecrypt = False
isBlock = False
isEncryptUV = True
imgPath = './lena.jpeg'
M = 800
N = 800

# 使用CV2包得到的不是通常的RGB顺序，而是BGR顺序，在上述代码第3行中可将图片颜色空间由BGR转为RGB
def read_image(path):
    image = cv2.imread(path)  # bgr
    image = image[:, :, ::-1]  # rgb
    return image

def show_image(image):
    plt.imshow(image)
    plt.show()

if __name__ == '__main__':
    rgb_image = cv2.imread(imgPath)
    cv2.imshow('src',rgb_image)
    cv2.waitKey(0)
    # 色彩空间转换
    ycbcr_image = cv2.cvtColor(rgb_image, cv2.COLOR_BGR2YUV)
    cv2.imshow('dst',ycbcr_image)
    cv2.waitKey(0)
    img_originalData_Y_before = ycbcr_image[:, :, 0]
    img_originalData_U = ycbcr_image[:, :, 1]
    img_originalData_V = ycbcr_image[:, :, 2]
    # dct变换
    img_originalData_Y_before = img_originalData_Y_before.astype(np.float32)
    img_originalData_Y = cv2.dct(img_originalData_Y_before)
    img_originalData_Y = img_originalData_Y.astype(np.uint8)
    ciphertext_Y = img_originalData_Y
    ciphertext_U = img_originalData_U
    ciphertext_V = img_originalData_V
    ciphertext_Y_after = ciphertext_Y

    # ROI裁剪
    img_part = img_originalData_U[0:100, 0:100]
    ceil_imgU = []
    ceil_imgV = []
    ceil_imgY = []
    for j in range(0,int(N/8)):
        for i in range(0,int(M/8)):
            rectU = img_originalData_U[8*i:8*i+8, 8*j:8*j+8]
            ceil_imgU.append(rectU)
            rectV = img_originalData_U[8*i:8*i+8, 8*j:8*j+8]
            ceil_imgV.append(rectV)
            if isBlock:
                rectY = img_originalData_Y[8 * i:8 * i + 8, 8 * j:8 * j + 8]
                ceil_imgY.append(rectY)

    # 种子密钥k
    random.seed(1234)
    # 异或
    mask = np.zeros([8,8], np.uint8)
    for pixel in range(0,int(M*N/64)):
        # 对每块做异或
        if random.randint(0,2) == 0:
            mask = np.zeros([8,8], np.uint8) * 0b00000000
        else:
            mask = np.zeros([8,8], np.uint8) * 0b11111111

        ceil_imgU[pixel] = cv2.bitwise_xor(ceil_imgU[pixel], mask)
        ceil_imgV[pixel] = cv2.bitwise_xor(ceil_imgU[pixel], mask)
        if isBlock:
            ceil_imgY[pixel] = cv2.bitwise_xor(ceil_imgY[pixel], mask)
        # 对每块做翻转
        temp_judge = random.randint(0,4)
        if temp_judge == 0:
            ceil_imgU[pixel] = cv2.rotate(ceil_imgU[pixel],cv2.ROTATE_90_CLOCKWISE)
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

    # 置换加密
    ceil_imgU = random.sample(ceil_imgU,len(ceil_imgU))
    ceil_imgV = random.sample(ceil_imgV, len(ceil_imgV))
    if isBlock:
        ceil_imgY = random.sample(ceil_imgY, len(ceil_imgY))
    for j in range(0,int(N/8)):
        for i in range(0,int(M/8)):
            ciphertext_U[8*i:8*i+8, 8*j:8*j+8] = ceil_imgU[i + j*8]
            ciphertext_V[8*i:8*i+8, 8*j:8*j+8] = ceil_imgV[i + j*8]
            if isBlock:
                ciphertext_Y[8 * i:8 * i + 8, 8 * j:8 * j + 8] = ceil_imgY[i + j * 8]


    Key_1 = np.random.randint(0, 255, size=[M, N],dtype=np.uint8)

    if not isBlock:
        ciphertext_Y = cv2.bitwise_xor(img_originalData_Y, Key_1)
        ciphertext_Y = ciphertext_Y.astype(np.float32)
        ciphertext_Y_after = cv2.idct(ciphertext_Y)
        ciphertext_Y_after = ciphertext_Y_after.astype(np.uint8)

    ciphertext_YUV = cv2.merge([ciphertext_Y_after, ciphertext_U, ciphertext_V])

    cv2.imshow('dst_encrypt', ciphertext_YUV)
    cv2.waitKey(0)
    decryptedtext_Y = ciphertext_Y_after
    decryptedtext_U = ciphertext_U
    decryptedtext_V = ciphertext_V
    if not isEncryptUV:
        origin = cv2.cvtColor(rgb_image, cv2.COLOR_BGR2YUV)
        decryptedtext_U = origin[:, :, 1]
        decryptedtext_V = origin[:, :, 2]

    # result_image = cv2.cvtColor(ycbcr_image, cv2.COLOR_YUV2BGR)
    # cv2.imshow('result',result_image)
    # cv2.waitKey(0)

    if isDecrypt:
        # 解密
        ciphertext_Y_after = ciphertext_Y_after.astype(np.float32)
        ciphertext_Y_after_dct = cv2.dct(ciphertext_Y_after)
        ciphertext_Y_after_dct = ciphertext_Y_after_dct.astype(np.uint8)
        decryptedtext_Y_before = cv2.bitwise_xor(ciphertext_Y_after_dct, Key_1)  # 将密文的B通道与密钥进行异或运算
        decryptedtext_Y_before = decryptedtext_Y_before.astype(np.float32)
        decryptedtext_Y = cv2.idct(decryptedtext_Y_before)
        decryptedtext_Y = decryptedtext_Y.astype(np.uint8)

    decryptedtext_YUV = cv2.merge([decryptedtext_Y, decryptedtext_U, decryptedtext_V])  # 合并通道
    result_decrypt = cv2.cvtColor(decryptedtext_YUV, cv2.COLOR_YUV2BGR)
    cv2.imshow('Y',decryptedtext_Y)
    cv2.imshow('U', decryptedtext_U)
    cv2.imshow('V', decryptedtext_V)
    cv2.imshow("Decryptedtext", result_decrypt)  # 显示已解密的图片
    cv2.waitKey(0)