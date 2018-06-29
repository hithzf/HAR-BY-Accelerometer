# HAR-BY-Accelerometer
This Android APP can be used to collect human motion data and recognize human daily activities

### How to use
1.Upzip 'ARecognition.zip' to the root directory of the external storage of your Android cellphone;<br/>
2.Install this app to your cellphone;<br/>
3.Your cellphone should be placed in the right thigh pocket, it is worth noting that the mobile phone screen is touched to the thighs;<br/>
4.Click on the triangle on the main interface to start activity recognition (using SVM classifier);<br/>
5.The activity recognition application will run until you clear it from the background.<br/>

### File structure of 'ARecognition.zip'
>`ARecognition`
>>`battery.txt` Record battery power <br/>
>>`center20.txt` The center point of each category when sampling freq is 20 Hz <br/>
>>`center50.txt` The center point of each category when sampling freq is 50 Hz <br/>
>>`cluster.txt` Clustering result <br/>
>>`feature20.txt` Training data when sampling freq is 20 Hz <br/>
>>`feature50.txt` Training data when sampling freq is 50 Hz <br/>
>>`model20.txt` SVM model when sampling freq is 20 Hz <br/>
>>`model50.txt` SVM model when sampling freq is 50 Hz <br/>
>>`norm20.txt` Normalized parameters when sampling freq is 20 Hz <br/>
>>`norm50.txt` Normalized parameters when sampling freq is 50 Hz <br/>
>>`result.txt` SVM classification result <br/>
>>`test.txt` SVM classification feature vector <br/>
>>`threshold20_50per.txt` Pre-classification threshold <br/>
>>`threshold20_90per.txt` Pre-classification threshold <br/>
