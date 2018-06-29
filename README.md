# HAR-BY-Accelerometer
This Android APP can be used to collect human motion data and recognize human daily activities

### How to use
1.Upzip 'ARecognition.zip' to the root directory of the external storage of your Android cellphone;
2.Install this app to your cellphone;
3.Your cellphone should be placed in the right thigh pocket, it is worth noting that the mobile phone screen is touched to the thighs;
4.Click on the triangle on the main interface to start activity recognition (using SVM classifier);
5.The activity recognition application will run until you clear it from the background.

### File structure of 'ARecognition.zip'
|-`ARecognition`
	|-`battery.txt` Record battery power
	|-`center20.txt` The center point of each category when sampling freq is 20 Hz
	|-`center50.txt` The center point of each category when sampling freq is 50 Hz
	|-`cluster.txt` Clustering result
	|-`feature20.txt` Training data when sampling freq is 20 Hz
	|-`feature50.txt` Training data when sampling freq is 50 Hz
	|-`model20.txt` SVM model when sampling freq is 20 Hz
	|-`model50.txt` SVM model when sampling freq is 50 Hz
	|-`norm20.txt` Normalized parameters when sampling freq is 20 Hz
	|-`norm50.txt` Normalized parameters when sampling freq is 50 Hz
	|-`result.txt` SVM classification result
	|-`test.txt` SVM classification feature vector
	|-`threshold20_50per.txt` Pre-classification threshold
	|-`threshold20_90per.txt` Pre-classification threshold
