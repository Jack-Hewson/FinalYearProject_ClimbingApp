# FinalYearProject_ClimbingApp
Computer Science Final Year Project

Android Application using a trained model to detect and label different types of climbing holds

# Training the Model
## Setup
I recommend using Anaconda since you will need to revert to an older version of Python and Tensorflow                                 
[Click here to download Anaconda](https://www.anaconda.com/distribution/#download-section)

| Package Name  | Version |
| ------------- | ------------- |
| pip | 20.0.2 |
| python | 3.5|
| protobug  | 3.11.3 |
| pillow  | 7.0.0 |
| lxml | 4.5.0 |
| cython | 0.29.14 |
| contextlib2 | 0.6.0 |
| jupyter | 1.0.0 |
| matplotlib | 3.1.3 |
| pandas | 1.0.1 |
| opencv-python | 4.2.0.32 |

### Tensorflow GPU
I have a CUDA-Enabled GPU (2080 Super) and so I am able to use Tensorflow-GPU which trains the model around 100 times faster.
To use Tensorflow-GPU:
- Check if you have a [CUDA-Enabled GPU](https://developer.nvidia.com/cuda-gpus)
- Install/update your [GPU drivers](https://www.nvidia.com/download/index.aspx?lang=en-us)
- Install [CUDA Toolkit](https://developer.nvidia.com/cuda-toolkit-archive)
- Install [cuDNN](https://developer.nvidia.com/cudnn)

| Package Name  | Version |
| ------------- | ------------- |
| cudnn | 7.6.5 |
| tensorflow-gpu | 2.1.0 |
| GPU Drivers| 442.50 |

If you do not have a CUDA-Enabled GPU then you can simply install tensorflow

| Package Name  | Version |
| ------------- | ------------- |
| tensorflow | 1.13.1 |
