# Description
This is a demo application showing possible solutions for how you can achieve blending effect of a colored overlay with the Android device Homescreen. It applies a random color overlay and one of 4 [Purter-Duff](http://ssp.impulsetrain.com/porterduff.html) effects whenever a new touch event happens:
## [SRC](https://developer.android.com/reference/android/graphics/PorterDuff.Mode#SRC)
<img src="https://raw.githubusercontent.com/AlexandrSMed/stack-35590953/master/readmeAssets/SRC.png" width="256" alt="SRC">

## [ADD](https://developer.android.com/reference/android/graphics/PorterDuff.Mode#ADD)
<img src="https://raw.githubusercontent.com/AlexandrSMed/stack-35590953/master/readmeAssets/ADD.png" width="256" alt="SRC">

## [MULTIPLY](https://developer.android.com/reference/android/graphics/PorterDuff.Mode#MULTIPLY)
<img src="https://raw.githubusercontent.com/AlexandrSMed/stack-35590953/master/readmeAssets/MULTIPLY.png" width="256" alt="MULTIPLY">

## [XOR](https://developer.android.com/reference/android/graphics/PorterDuff.Mode#XOR)
<img src="https://raw.githubusercontent.com/AlexandrSMed/stack-35590953/master/readmeAssets/XOR.png" width="256" alt="XOR">

## Recording
The overal program cosists of a foreground service that adds overlay-like view at the system-alert level. Here is a small demo:

<img src="https://raw.githubusercontent.com/AlexandrSMed/stack-35590953/master/readmeAssets/video.gif" width="256" alt="recording">
