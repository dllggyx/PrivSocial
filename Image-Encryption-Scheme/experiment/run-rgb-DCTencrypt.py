import rgb_DCTencrypt

if __name__ == '__main__':
    # for idx in range(1, 21):
    idx = 7

    rgb_DCTencrypt.image_name = 'square' + str(idx) + '.jpeg'
    rgb_DCTencrypt.main()