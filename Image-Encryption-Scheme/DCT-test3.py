import cv2
import numpy as np

Qy = np.array([[16, 11, 10, 16, 24, 40, 51, 61],
                   [12, 12, 14, 19, 26, 58, 60, 55],
                   [14, 13, 16, 24, 40, 57, 69, 56],
                   [14, 17, 22, 29, 51, 87, 80, 62],
                   [18, 22, 37, 56, 68, 109, 103, 77],
                   [24, 35, 55, 64, 81, 104, 113, 92],
                   [49, 64, 78, 87, 103, 121, 120, 101],
                   [72, 92, 95, 98, 112, 100, 103, 99]], dtype=np.uint8)

def dct_block(image):
    (w, h) = image.shape
    image = image.astype(np.float32)
    image = image - 128
    result_image = image
    for j in range(0, int(w / 8)):
        for i in range(0, int(h / 8)):
            rect = image[8 * i:8 * i + 8, 8 * j:8 * j + 8]
            rect_dct = cv2.dct(rect)
            rect_dct = np.round(rect_dct / Qy)
            result_image[8 * i:8 * i + 8, 8 * j:8 * j + 8] = rect_dct
    return result_image


def idct_block(image):
    (w, h) = image.shape
    image = image.astype(np.float32)
    result_image = image
    for j in range(0, int(w / 8)):
        for i in range(0, int(h / 8)):
            rect = image[8 * i:8 * i + 8, 8 * j:8 * j + 8]
            rect_idct = cv2.idct(rect)
            rect_idct = rect_idct + 128
            result_image[8 * i:8 * i + 8, 8 * j:8 * j + 8] = rect_idct
    return result_image



if __name__ == '__main__':
    src = cv2.imread('./square3.jpeg', 0)
    src = src.astype(np.float32)
    temp = dct_block(src)
    cv2.imshow('temp.jpeg', temp.astype(np.uint8))
    temp = temp.astype(np.uint8)
    dst = idct_block(temp)
    cv2.imshow('dst', dst.astype(np.uint8))
    cv2.waitKey(0)