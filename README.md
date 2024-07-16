# PrivSocial
Artifacts for "How to Prevent Social Media Platforms from Knowing the Images You Share with Friends".

The basic components of PrivSocial are as follows:
|Directory|Description|
|:---:|:----:|
|CGKA|Continuous Group Key Agreement protocol code based on the ratchet tree, including both the relay server and client-side code|
|Image-Encryption-Scheme|Compression-resistant image encryption scheme code|
|Priv-raster|An application deployable on the user's Android device that participates in key agreement and image decryption processes|

## CGKA
The CGKA directory contains the basic TreeKEM implemented by Java, where submitted messages are forwarded by the relay server. 
你需要通过如下命令来运行代码:
1. Start the Relay Server:
```
cd CGKA/BTE-CGKA-v3/src
javac network.MyServer.java
java network.MyServer
```

2. Configure Client-Side Code (Main.java):

Set the appropriate group operations and group size:
```java
cgkaTest.test_create(16);
cgkaTest.test_remove(8);
cgkaTest.test_add1(256);
cgkaTest.test_add2(64);
```

3. Start the Client:
```
javac Main.java
java Main
```


## Image-Encryption-Scheme
This part contains the code for the compression-resistant image encryption scheme proposed by the paper. You can run the following command to see its effect:
```
python ./experiment/rgb_DCTencrypt.py
```
You can modify the configuration information by editing the `config.ini` file.


## Priv-raster
Priv-raster runs in Android, and you need to align the mask block with the image on your phone to obtain the correct decryption result. You can use Android Studio to compile the code and get the APK file.