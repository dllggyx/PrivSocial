import cv2
from configparser import ConfigParser
import numpy as np

import DCT_test2
import myencode,mydecode
from PIL import Image

# 初始化解析器
config = ConfigParser()
config.read('../config.ini', encoding='utf-8')
# 读取配置文件
decimalNum = config.getint('GlobalVars', 'decimalNum')
image_name = config.get('GlobalVars', 'image_name')
key = config.getint('GlobalVars', 'key')
keyY = config.getint('GlobalVars', 'keyY')
imgPath = '../result/src/' + image_name


if __name__ == '__main__':
    # myencode.encode('./img/source.jpeg','./img/encode.jpeg')
    # mydecode.decode('./img/encode.jpeg')
    rgb_image = cv2.imread('./img/square1.jpeg')
    M,N,_ = rgb_image.shape
    ycbcr_image = cv2.cvtColor(rgb_image, cv2.COLOR_BGR2YUV)
    img_y = ycbcr_image[:, :, 0]

    img_dct_Y = DCT_test2.dct_block(img_y, keyY)
    if decimalNum > 20:
        ciphertext_Y_after = img_dct_Y
    else:
        ciphertext_Y_after = np.round(img_dct_Y, decimals=0)

    ciphertext_Y_after += 128

    cv2.imwrite('encode_img/twitter_before.jpeg', ciphertext_Y_after.astype(np.uint8), [int(cv2.IMWRITE_JPEG_QUALITY), 50])

    y_load = cv2.imread('encode_img/twitter_before.jpeg', cv2.IMREAD_GRAYSCALE)
    cv2.imshow('y_load', y_load)

    y_load = y_load.astype(np.float32)
    y_load = y_load - 128

    Y_after_compress = DCT_test2.compress_before_idct(y_load)
    decryptedtext_Y = DCT_test2.idct_block_decompress(Y_after_compress, keyY)

    # decryptedtext_Y = DCT_test2.idct_block_decompress(y_load, keyY)

    decryptedtext_Y = decryptedtext_Y.astype(np.uint8)

    cv2.imshow('decryptedtext_Y', decryptedtext_Y)

    cv2.waitKey(0)



