import cv2
import matplotlib.pyplot as plt
import numpy as np

# 使用CV2包得到的不是通常的RGB顺序，而是BGR顺序，在上述代码第3行中可将图片颜色空间由BGR转为RGB
def read_image(path):
    image = cv2.imread(path)  # bgr
    image = image[:, :, ::-1]  # rgb
    return image

def show_image(image):
    plt.imshow(image)
    plt.show()

if __name__ == '__main__':
    rgb_image = cv2.imread('./square3.jpeg')
    cv2.imshow('src',rgb_image)
    cv2.waitKey(0)
    ycbcr_image = cv2.cvtColor(rgb_image, cv2.COLOR_BGR2YUV)
    cv2.imshow('dst',ycbcr_image)
    cv2.waitKey(0)
    print(ycbcr_image.shape)
    img_originalData_Y = ycbcr_image[:, :, 0]
    img_originalData_U = ycbcr_image[:, :, 1]
    img_originalData_V = ycbcr_image[:, :, 2]
    ciphertext_Y = img_originalData_Y
    ciphertext_U = img_originalData_U
    ciphertext_V = img_originalData_V

    cv2.imshow('0',ciphertext_Y)
    cv2.imshow('1', ciphertext_U)
    cv2.imshow('2', ciphertext_V)

    Key_1 = np.random.randint(0, 255, size=[800, 800],dtype=np.uint8)
    # ciphertext_Y = cv2.bitwise_xor(img_originalData_Y, Key_1)
    # ciphertext_U = cv2.bitwise_xor(img_originalData_U, Key_1)
    ciphertext_V = cv2.bitwise_xor(img_originalData_V, Key_1)

    ciphertext_YUV = cv2.merge([ciphertext_Y, ciphertext_U, ciphertext_V])

    cv2.imshow('dst_encrypt', ciphertext_YUV)
    cv2.waitKey(0)
    decryptedtext_Y = ciphertext_Y
    decryptedtext_U = ciphertext_U
    decryptedtext_V = ciphertext_V

    # result_image = cv2.cvtColor(ycbcr_image, cv2.COLOR_YUV2BGR)
    # cv2.imshow('result',result_image)
    # cv2.waitKey(0)

    # 解密
    # decryptedtext_Y = cv2.bitwise_xor(ciphertext_Y, Key_1)  # 将密文的B通道与密钥进行异或运算
    # decryptedtext_U = cv2.bitwise_xor(ciphertext_U, Key_1)  # 将密文的G通道与密钥进行异或运算
    # decryptedtext_V = cv2.bitwise_xor(ciphertext_V, Key_1)  # 将密文的R通道与密钥进行异或运算
    decryptedtext_YUV = cv2.merge([decryptedtext_Y, decryptedtext_U, decryptedtext_V])  # 合并通道
    result_decrypt = cv2.cvtColor(decryptedtext_YUV, cv2.COLOR_YUV2BGR)
    cv2.imshow("Decryptedtext", result_decrypt)  # 显示已解密的图片
    cv2.waitKey(0)