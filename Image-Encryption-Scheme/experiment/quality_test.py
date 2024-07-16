import math

from skimage.metrics import peak_signal_noise_ratio as compare_psnr
from skimage.metrics import structural_similarity as compare_ssim
from skimage.metrics import mean_squared_error as compare_mse
import cv2
import numpy as np
import matplotlib.pyplot as plt
import csv
import os

'''
https://blog.csdn.net/qq_34848334/article/details/128142615?ops_request_misc=&request_id=&biz_id=102&utm_term=skimage.metrics%20ssim&utm_medium=distribute.pc_search_result.none-task-blog-2~all~sobaiduweb~default-3-128142615.142^v100^pc_search_result_base5&spm=1018.2226.3001.4187
'''

#从direct1里搜索源图片
#压缩存到direct2里
#把加密结果存到direct3中
#把解密结果存到direct4中

src_dir = ''
compress_dir = ''
encrypt_dir = ''
#经过EtC
decrypt_dir = ''



'''
1 用来测src和compress
2 用来测src和块加密
3 用来测src和抗压缩加密

'''


psnr1_path = '../result/' + 'psnr1.csv'
psnr2_path = '../result/' + 'psnr2.csv'
psnr3_path = '../result/' + 'psnr3.csv'
ssim1_path = '../result/' + 'ssim1.csv'
ssim2_path = '../result/' + 'ssim2.csv'
ssim3_path = '../result/' + 'ssim3.csv'
mse1_path = '../result/' + 'mse1.csv'
mse2_path = '../result/' + 'mse2.csv'
mse3_path = '../result/' + 'mse3.csv'
average_path = '../result/' + 'average.csv'
average = [0] * 9


def write_to_csv(path, data):
    with open(path, 'w', newline='') as file:
        file.write(data)


'''
分别计算图像通道相邻像素的水平、垂直和对角线的相关系数并返回
'''


def RGB_correlation(channel, N):
    # 计算channel通道
    h, w = channel.shape
    # 随机产生pixels个[0,w-1)范围内的整数序列
    row = np.random.randint(0, h - 1, N)
    col = np.random.randint(0, w - 1, N)
    # 绘制相邻像素相关性图,统计x,y坐标
    x = []
    h_y = []
    v_y = []
    d_y = []
    for i in range(N):
        # 选择当前一个像素
        x.append(channel[row[i]][col[i]])
        # 水平相邻像素是它的右侧也就是同行下一列的像素
        h_y.append(channel[row[i]][col[i] + 1])
        # 垂直相邻像素是它的下方也就是同列下一行的像素
        v_y.append(channel[row[i] + 1][col[i]])
        # 对角线相邻像素是它的右下即下一行下一列的那个像素
        d_y.append(channel[row[i] + 1][col[i] + 1])
    # 三个方向的合到一起
    x = x * 3
    y = h_y + v_y + d_y

    # 结果展示
    # plt.rcParams['font.sans-serif'] = ['SimHei']  # 中文乱码
    # plt.scatter(x,y)
    # plt.show()

    # 计算E(x)，计算三个方向相关性时，x没有重新选择也可以更改
    ex = 0
    for i in range(N):
        ex += channel[row[i]][col[i]]
    ex = ex / N
    # 计算D(x)
    dx = 0
    for i in range(N):
        dx += (channel[row[i]][col[i]] - ex) ** 2
    dx /= N

    # 水平相邻像素h_y
    # 计算E(y)
    h_ey = 0
    for i in range(N):
        h_ey += channel[row[i]][col[i] + 1]
    h_ey /= N
    # 计算D(y)
    h_dy = 0
    for i in range(N):
        h_dy += (channel[row[i]][col[i] + 1] - h_ey) ** 2
    h_dy /= N
    # 计算协方差
    h_cov = 0
    for i in range(N):
        h_cov += (channel[row[i]][col[i]] - ex) * (channel[row[i]][col[i] + 1] - h_ey)
    h_cov /= N
    h_Rxy = h_cov / (np.sqrt(dx) * np.sqrt(h_dy))

    # 垂直相邻像素v_y
    # 计算E(y)
    v_ey = 0
    for i in range(N):
        v_ey += channel[row[i] + 1][col[i]]
    v_ey /= N
    # 计算D(y)
    v_dy = 0
    for i in range(N):
        v_dy += (channel[row[i] + 1][col[i]] - v_ey) ** 2
    v_dy /= N
    # 计算协方差
    v_cov = 0
    for i in range(N):
        v_cov += (channel[row[i]][col[i]] - ex) * (channel[row[i] + 1][col[i]] - v_ey)
    v_cov /= N
    v_Rxy = v_cov / (np.sqrt(dx) * np.sqrt(v_dy))

    # 对角线相邻像素d_y
    # 计算E(y)
    d_ey = 0
    for i in range(N):
        d_ey += channel[row[i] + 1][col[i] + 1]
    d_ey /= N
    # 计算D(y)
    d_dy = 0
    for i in range(N):
        d_dy += (channel[row[i] + 1][col[i] + 1] - d_ey) ** 2
    d_dy /= N
    # 计算协方差
    d_cov = 0
    for i in range(N):
        d_cov += (channel[row[i]][col[i]] - ex) * (channel[row[i] + 1][col[i] + 1] - d_ey)
    d_cov /= N
    d_Rxy = d_cov / (np.sqrt(dx) * np.sqrt(d_dy))

    return h_Rxy, v_Rxy, d_Rxy, x, y


'''
分别计算图像img的各通道相邻像素的相关系数，默认随机选取3000对相邻像素
'''


def correlation(img, N=3000):
    img = cv2.imread(img)
    h, w, _ = img.shape
    B, G, R = cv2.split(img)
    R_Rxy = RGB_correlation(R, N)
    G_Rxy = RGB_correlation(G, N)
    B_Rxy = RGB_correlation(B, N)

    # # 结果展示
    # plt.rcParams['font.sans-serif'] = ['SimHei']  # 中文乱码
    # plt.subplot(221)
    # plt.imshow(img[:, :, (2, 1, 0)])
    # plt.title('原图像')
    # # 子图2
    # plt.subplot(222)
    # plt.scatter(R_Rxy[3], R_Rxy[4], s=1, c='red')
    # plt.title('通道R')
    #
    # # 子图3
    # plt.subplot(223)
    # plt.scatter(G_Rxy[3], G_Rxy[4], s=1, c='green')
    # plt.title('通道G')
    # # 子图4
    # plt.subplot(224)
    # plt.scatter(B_Rxy[3], B_Rxy[4], s=1, c='blue')
    # plt.title('通道B')
    # plt.show()

    return R_Rxy, G_Rxy, B_Rxy




def obtain_quality():
    res_psnr1 = ''
    res_psnr2 = ''
    res_psnr3 = ''
    res_ssim1 = ''
    res_ssim2 = ''
    res_ssim3 = ''
    res_mse1 = ''
    res_mse2 = ''
    res_mse3 = ''
    terminate = ','
    for idx in range(1, 21):
        src_path = '../result/src/square' + str(idx) + '.jpeg'
        com_path = '../result/com/square' + str(idx) + '.jpeg'
        block_path = '../result/block/square' + str(idx) + '.jpeg'
        ourscheme_path = '../result/ourscheme/square' + str(idx) + '.jpeg'

        src = cv2.imread(src_path)
        com = cv2.imread(com_path)
        block = cv2.imread(block_path)
        ourscheme = cv2.imread(ourscheme_path)

        if idx == 20:
            terminate = ''

        value1 = compare_psnr(src, com, data_range=255)
        value2 = compare_ssim(src, com, channel_axis=-1)
        value3 = compare_mse(src, com)
        res_psnr1 += str(value1) + terminate
        res_ssim1 += str(value2) + terminate
        res_mse1 += str(value3) + terminate
        average[0] += value1
        average[1] += value2
        average[2] += value3

        value4 = compare_psnr(src, block, data_range=255)
        value5 = compare_ssim(src, block, channel_axis=-1)
        value6 = compare_mse(src, block)
        res_psnr2 += str(value4) + terminate
        res_ssim2 += str(value5) + terminate
        res_mse2 += str(value6) + terminate
        average[3] += value4
        average[4] += value5
        average[5] += value6

        value7 = compare_psnr(src, ourscheme, data_range=255)
        value8 = compare_ssim(src, ourscheme, channel_axis=-1)
        value9 = compare_mse(src, ourscheme)
        res_psnr3 += str(value7) + terminate
        res_ssim3 += str(value8) + terminate
        res_mse3 += str(value9) + terminate
        average[6] += value7
        average[7] += value8
        average[8] += value9

    write_to_csv(psnr1_path, res_psnr1)
    write_to_csv(ssim1_path, res_ssim1)
    write_to_csv(mse1_path, res_mse1)

    write_to_csv(psnr2_path, res_psnr2)
    write_to_csv(ssim2_path, res_ssim2)
    write_to_csv(mse2_path, res_mse2)

    write_to_csv(psnr3_path, res_psnr3)
    write_to_csv(ssim3_path, res_ssim3)
    write_to_csv(mse3_path, res_mse3)

    result = ''
    for i in range(0,9):
        result += str(average[i]/20) + '\n'
    write_to_csv(average_path, result)


def encrypt_quality(img_path):
    R_Rxy, G_Rxy, B_Rxy = correlation(img_path)
    # # 输出结果保留四位有效数字
    # print("******该图像的各通道各方向的相关系数为*****")
    # print('通道\tHorizontal\tVertical\tDiagonal')
    # print(' R    \t{:.4f}    {:.4f}    {:.4f}'.format(R_Rxy[0], R_Rxy[1], R_Rxy[2]))
    # print(' G    \t{:.4f}    {:.4f}    {:.4f}'.format(G_Rxy[0], G_Rxy[1], G_Rxy[2]))
    # print(' B    \t{:.4f}    {:.4f}    {:.4f}'.format(B_Rxy[0], B_Rxy[1], B_Rxy[2]))
    return R_Rxy, G_Rxy, B_Rxy


'''
计算图像的信息熵
'''


def entropy(img):
    img = cv2.imread(img)
    w, h, _ = img.shape
    B, G, R = cv2.split(img)
    gray, num1 = np.unique(R, return_counts=True)
    gray, num2 = np.unique(G, return_counts=True)
    gray, num3 = np.unique(B, return_counts=True)
    R_entropy = 0
    G_entropy = 0
    B_entropy = 0


    for i in range(min([len(gray), len(num1), len(num2), len(num3)])):
        p1 = num1[i] / (w * h)
        p2 = num2[i] / (w * h)
        p3 = num3[i] / (w * h)
        R_entropy -= p1 * (math.log(p1, 2))
        G_entropy -= p2 * (math.log(p2, 2))
        B_entropy -= p3 * (math.log(p3, 2))
    return [R_entropy, G_entropy, B_entropy]


'''
计算加密质量，img1是原图，img2是加密图像
'''
def EQ(img1,img2):
  img1=cv2.imread(img1)
  img2=cv2.imread(img2)
  w,h,_=img1.shape
  B1,G1,R1=cv2.split(img1)
  B2,G2,R2=cv2.split(img2)
  R1_H={}
  R2_H={}
  G1_H={}
  G2_H={}
  B1_H={}
  B2_H={}
  R_EQ=0
  G_EQ=0
  B_EQ=0
  for i in range(256):
    R1_H[i]=0
    R2_H[i]=0
    G1_H[i]=0
    G2_H[i]=0
    B1_H[i]=0
    B2_H[i]=0

  for i in range(w):
    for j in range(h):
      R1_H[R1[i][j]]+=1
      R2_H[R2[i][j]]+=1
      G1_H[G1[i][j]]+=1
      G2_H[G2[i][j]]+=1
      B1_H[B1[i][j]]+=1
      B2_H[B2[i][j]]+=1

  for i in range(256):
    #公式里是平方，待考虑
    R_EQ+=abs(R1_H[i]-R2_H[i])
    G_EQ+=abs(G1_H[i]-G2_H[i])
    B_EQ+=abs(B1_H[i]-B2_H[i])
  # print(R_EQ)
  R_EQ/=256
  G_EQ/=256
  B_EQ/=256
  return R_EQ,G_EQ,B_EQ


def obtain_encrypt_message(select):
    # idx = 1
    eq1 = 0
    eq2 = 0
    eq3 = 0
    entropy1 = 0
    entropy2 = 0
    entropy3 = 0
    entropy4 = 0

    cov1 = [[0, 0, 0], [0, 0, 0], [0, 0, 0]]
    cov2 = [[0, 0, 0], [0, 0, 0], [0, 0, 0]]
    cov3 = [[0, 0, 0], [0, 0, 0], [0, 0, 0]]
    cov4 = [[0, 0, 0], [0, 0, 0], [0, 0, 0]]

    cov1_full = None
    cov2_full = None
    cov3_full = None
    cov4_full = None

    cov1_average = 0
    cov2_average = 0
    cov3_average = 0
    cov4_average = 0

    cov_average1_x = []
    cov_average1_y = []
    cov_average2_x = []
    cov_average2_y = []
    cov_average3_x = []
    cov_average3_y = []
    cov_average4_x = []
    cov_average4_y = []


    result = {}


    for idx in range(1, 21):
        src_path = '../result/src/square' + str(idx) + '.jpeg'
        en_block_path = '../result/block_enc/square' + str(idx) + '.jpeg'
        en_ourscheme_path = '../result/ourscheme_enc/square' + str(idx) + '.jpeg'
        en_aes_path = '../result/aes_enc/square' + str(idx) + '.jpeg'

        if select == 0:
            ''' 相关系数评判 '''
            cov1_full = encrypt_quality(src_path)
            cov2_full = encrypt_quality(en_block_path)[0:3]
            cov3_full = encrypt_quality(en_ourscheme_path)[0:3]
            cov4_full = encrypt_quality(en_aes_path)[0:3]
            cov1_temp = cov1_full[0:3]
            cov2_temp = cov2_full[0:3]
            cov3_temp = cov3_full[0:3]
            cov4_temp = cov4_full[0:3]
            first_flag = True
            for rgb_iter in range(0, 3):
                for iter in range(0, 3):
                    first_flag = False
                    cov1[rgb_iter][iter] += cov1_temp[rgb_iter][iter]
                    cov2[rgb_iter][iter] += cov2_temp[rgb_iter][iter]
                    cov3[rgb_iter][iter] += cov3_temp[rgb_iter][iter]
                    cov4[rgb_iter][iter] += cov4_temp[rgb_iter][iter]

                if idx == 17:
                    cov_average1_x = cov1_full[rgb_iter][3]
                    cov_average1_y = cov1_full[rgb_iter][4]
                    cov_average2_x = cov2_full[rgb_iter][3]
                    cov_average2_y = cov2_full[rgb_iter][4]
                    cov_average3_x = cov3_full[rgb_iter][3]
                    cov_average3_y = cov3_full[rgb_iter][4]
                    cov_average4_x = cov4_full[rgb_iter][3]
                    cov_average4_y = cov4_full[rgb_iter][4]

            # #再测个AES的
        elif select == 1:
            ''' 信息熵 '''
            ''' 三通道平均值 '''
            R_entropy, G_entropy, B_entropy = entropy(src_path)
            entropy1 = entropy1 + R_entropy + G_entropy + B_entropy
            R_entropy, G_entropy, B_entropy = entropy(en_block_path)
            entropy2 = entropy2 + R_entropy + G_entropy + B_entropy
            R_entropy, G_entropy, B_entropy = entropy(en_ourscheme_path)
            entropy3 = entropy3 + R_entropy + G_entropy + B_entropy
            R_entropy, G_entropy, B_entropy = entropy(en_aes_path)
            entropy4 = entropy4 + R_entropy + G_entropy + B_entropy

        elif select == 2:
            ''' 加密质量 '''
            ''' 算三通道平均值 '''
            R_EQ, G_EQ, B_EQ = EQ(src_path, en_block_path)
            eq1 = eq1 + R_EQ + G_EQ + B_EQ
            # print('***********EQ*********')
            # print(R_EQ, G_EQ, B_EQ)
            # print('通道R:{:.0f}'.format(R_EQ))
            # print('通道G:{:.0f}'.format(G_EQ))
            # print('通道B:{:.0f}'.format(B_EQ))

            R_EQ, G_EQ, B_EQ = EQ(src_path, en_ourscheme_path)
            eq2 = eq2 + R_EQ + G_EQ + B_EQ
            # print('***********EQ*********')
            # print(R_EQ, G_EQ, B_EQ)
            # print('通道R:{:.0f}'.format(R_EQ))
            # print('通道G:{:.0f}'.format(G_EQ))
            # print('通道B:{:.0f}'.format(B_EQ))
            R_EQ, G_EQ, B_EQ = EQ(src_path, en_aes_path)
            eq3 = eq3 + R_EQ + G_EQ + B_EQ

            print("idx = " + str(idx))

    entropy1 = entropy1 / 60
    entropy2 = entropy2 / 60
    entropy3 = entropy3 / 60
    entropy4 = entropy4 / 60
    eq1 = eq1 / 20
    eq2 = eq2 / 20
    eq3 = eq3 / 20


    if select == 0:
        for rgb_iter in range(0, 3):
            for iter in range(0, 3):
                cov1[rgb_iter][iter] = cov1[rgb_iter][iter] / 20
                cov2[rgb_iter][iter] = cov2[rgb_iter][iter] / 20
                cov3[rgb_iter][iter] = cov3[rgb_iter][iter] / 20
                cov4[rgb_iter][iter] = cov4[rgb_iter][iter] / 20
                cov1_average += cov1[rgb_iter][iter]
                cov2_average += cov2[rgb_iter][iter]
                cov3_average += cov3[rgb_iter][iter]
                cov4_average += cov4[rgb_iter][iter]

        cov1_average = cov1_average / 9
        cov2_average = cov2_average / 9
        cov3_average = cov3_average / 9
        cov4_average = cov4_average / 9



        plt.scatter(cov_average1_x, cov_average1_y, s=1, c='blue')
        # plt.title('Original Image')
        plt.xticks(fontsize=22, fontweight='bold')
        plt.yticks(fontsize=22, fontweight='bold')
        plt.savefig('./cov-result/cov-original.png', dpi=800, bbox_inches="tight")
        plt.show()

        plt.scatter(cov_average2_x, cov_average2_y, s=1, c='green')
        # plt.title('Block-based Encryption')
        plt.xticks(fontsize=22, fontweight='bold')
        plt.yticks(fontsize=22, fontweight='bold')
        plt.savefig('./cov-result/cov-block.png', dpi=800, bbox_inches="tight")
        plt.show()

        plt.scatter(cov_average3_x, cov_average3_y, s=1, c='red')
        # plt.title('Proposed Scheme')
        plt.xticks(fontsize=22, fontweight='bold')
        plt.yticks(fontsize=22, fontweight='bold')
        plt.savefig('./cov-result/cov-ourscheme.png', dpi=800, bbox_inches="tight")
        plt.show()

        # plt.scatter(cov_average4_x, cov_average4_y, s=1, c='blue')
        # plt.title('aes')
        # plt.show()


    result["entropy_src"] = entropy1
    result["entropy_block"] = entropy2
    result["entropy_ourscheme"] = entropy3
    result["entropy_aes"] = entropy4
    result["eq1_block"] = eq1
    result["eq2_ourscheme"] = eq2
    result["eq2_aes"] = eq3
    result["cov_src"] = cov1_average
    result["cov_block"] = cov2_average
    result["cov_ourscheme"] = cov3_average
    result["cov_aes"] = cov4_average

    return result


if __name__ == '__main__':
    ''' 图片质量 '''
    # obtain_quality()


    # src_path = '../result/src/square6.jpeg'
    # src = cv2.imread(src_path,-1)
    # img1_path = '../img/Q_test/de_40square6.jpeg'
    # img2_path = '../img/Q_test/de_50square6.jpeg'
    # img3_path = '../img/Q_test/de_60square6.jpeg'
    # img4_path = '../img/Q_test/de_70square6.jpeg'
    # img5_path = '../img/Q_test/de_80square6.jpeg'
    # img6_path = '../img/Q_test/de_90square6.jpeg'
    # img1 = cv2.imread(img1_path, -1)
    # img2 = cv2.imread(img2_path, -1)
    # img3 = cv2.imread(img3_path, -1)
    # img4 = cv2.imread(img4_path, -1)
    # img5 = cv2.imread(img5_path, -1)
    # img6 = cv2.imread(img6_path, -1)
    # value1 = compare_psnr(src, img1, data_range=255)
    # value2 = compare_psnr(src, img2, data_range=255)
    # value3 = compare_psnr(src, img3, data_range=255)
    # value4 = compare_psnr(src, img4, data_range=255)
    # value5 = compare_psnr(src, img5, data_range=255)
    # value6 = compare_psnr(src, img6, data_range=255)
    # print([value1, value2, value3, value4, value5, value6])



    ''' 加密图片质量评价 '''
    res = obtain_encrypt_message(1)
    for key, value in res.items():
        print(f"{key}: {value}")


