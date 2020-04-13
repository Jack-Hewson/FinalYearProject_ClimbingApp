import pyrebase
import random
from object_detection import xml_to_csv

config = {
   "apiKey": "AIzaSyD-Pj8Jqgnip2goZCDq0X-3ETslPJzGPFE",
   "authDomain": "climbingapp-a89a9",
   "databaseURL": "https://climbingapp-a89a9.firebaseio.com/",
   "storageBucket": "climbingapp-a89a9.appspot.com",
   "serviceAccount":"C:/Users/jacks\OneDrive - University of Plymouth/_Fourth Year/PRCO304/climbingApp_ServiceAccount.json"
}

firebase = pyrebase.initialize_app(config)
storage = firebase.storage()
files = storage.list_files()
#filelist = list(files)
#random.shuffle(filelist)
xmlList = []
imagesList = []

#Assign the images and XML's to their respective list
for file in files:
    if not(file.name.endswith('/')):
      if ("xml/" in file.name):
         xmlList.append(file.name)
      elif ("images/" in file.name):
         imagesList.append(file.name)

random.shuffle(xmlList)
random.shuffle(imagesList)

testingQntImage = testingQntXML = int(len(xmlList) * 0.2)
trainingQntImage = trainingQntXML = len(xmlList) - testingQntXML

for xml in xmlList:
   if (testingQntXML > 0):
      print("Downloading XML file '" + xml + "' for testing")
      storage.child(xml).download("images/test/" + xml.split("/")[1])
      testingQntXML = testingQntXML - 1
   else:
      print("Downloading XML file '" + xml + "' for training")
      storage.child(xml).download("images/train/"+ xml.split("/")[1])
      trainingQntXML = trainingQntXML - 1

for image in imagesList:
   if (testingQntImage > 0):
      print("Downloading image file '" + image + "' for testing")
      storage.child(image).download("images/test/" + image.split("/")[1])
      testingQntImage = testingQntImage - 1
   else:
      print("Downloading image file '" + image + "' for training")
      storage.child(image).download("images/train/" + image.split("/")[1])
      trainingQntImage = trainingQntImage - 1

xml_to_csv.main()