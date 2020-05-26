import pyrebase
import random
import sys
import os
import datetime
import xml.etree.ElementTree as ET
sys.path.append("C:/Users/jacks/OneDrive - University of Plymouth/_Fourth Year/PRCO304/tensorflow1/models/research/slim")
sys.path.append("C:/Users/jacks/OneDrive - University of Plymouth/_Fourth Year/PRCO304/tensorflow1/models/research")
sys.path.append("C:/Program Files/NVIDIA GPU Computing Toolkit/CUDA/v10.1/bin")
sys.path.append("C:/Program Files/NVIDIA GPU Computing Toolkit/CUDA/v10.1/extras/CUPTI\libx64")
sys.path.append("C:/Program Files/NVIDIA GPU Computing Toolkit/CUDA/v10.1/include")
sys.path.append("C:/tools/cuda/bin")
from object_detection import xml_to_csv
from object_detection import train
from object_detection import export_tflite_ssd_graph
import tensorflow as tf
from tensorflow.lite.python import lite_constants
from object_detection import generate_tfrecord

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
xmlList = []
imagesList = []

def sortDownloaded():
   print("Downloading from Firebase Storage...")
   for file in files:
      if not(file.name.endswith('/')):
         if ("xml/" in file.name):
           xmlList.append(file.name)
         elif ("images/" in file.name):
          imagesList.append(file.name)

   print("Shuffling XML files for random assignment")
   random.shuffle(xmlList)

   testingQntXML = int(len(xmlList) * 0.2)
   print("files for testing = " + str(testingQntXML) + "/" + str(len(xmlList)))
   trainingQntXML = len(xmlList) - testingQntXML
   print("files for training = " + str(trainingQntXML) + "/" + str(len(xmlList)))

   for xml in xmlList:
      if (testingQntXML > 0):
         print("Downloading XML file '" + xml + "' for testing")
         storage.child(xml).download("images/test/" + xml.split("/")[1])
         storage.delete(xml)
         testingQntXML = testingQntXML - 1
      else:
         print("Downloading XML file '" + xml + "' for training")
         storage.child(xml).download("images/train/"+ xml.split("/")[1])
         storage.delete(xml)
         trainingQntXML = trainingQntXML - 1

   for image in imagesList:
      #Checks if the xml for the image is in test folder, if not then must be in train
      if (os.path.exists("images/test/" + (image.split("/")[1]).split(".")[0] + ".xml")):
         print("Downloading image file '" + image + "' for testing")
         storage.child(image).download("images/test/" + image.split("/")[1])
         storage.delete(image)
      else:
         print("Downloading image file '" + image + "' for training")
         storage.child(image).download("images/train/" + image.split("/")[1])
         storage.delete(image)

def addInfoToXml():
   folderLocation = "C:/Users/jacks/OneDrive - University of Plymouth/_Fourth Year/PRCO304/tensorflow1/models/research/object_detection/images/"
   folderLocationTest = folderLocation + "test/"
   folderLocationTrain = folderLocation + "train/"

   #AMEND TESTING FILES
   for filename in os.listdir(folderLocationTest):
      if filename.endswith(".xml"):
         print(filename)
         tree = ET.parse(folderLocationTest + filename)
         root = tree.getroot()
         for elem in root.iter('folder'):
            print("amending folder")
            new_value = "test"
            elem.text = new_value
         for elem in root.iter('path'):
            print("amending path")
            new_value = folderLocationTest + filename.split('.')[0] + ".jpg"
            elem.text = str(new_value)
         tree.write(folderLocationTest + filename)


   #AMEND TRAINING FILES
   for filename in os.listdir(folderLocationTrain):
      if filename.endswith(".xml"):
         print(filename)
         tree = ET.parse(folderLocationTrain + filename)
         root = tree.getroot()
         for elem in root.iter('folder'):
            print("amending folder")
            new_value = "train"
            elem.text = new_value
         for elem in root.iter('path'):
            print("amending path")
            new_value = folderLocationTrain + filename.split('.')[0] + ".jpg"
            elem.text = str(new_value)
         tree.write(folderLocationTrain + filename)

def generateTrainRecords():
   print("Creating TFRecords for Train...")
   generate_tfrecord.FLAGS.csv_input = "images/train_labels.csv"
   generate_tfrecord.FLAGS.image_dir = "images/train"
   generate_tfrecord.FLAGS.output_path = "train.record"
   generate_tfrecord.main()
   
def generateTestRecords():
   print("Creating TFRecords for Test...")
   generate_tfrecord.FLAGS.csv_input = "images/test_labels.csv"
   generate_tfrecord.FLAGS.image_dir = "images/test"
   generate_tfrecord.FLAGS.output_path = "test.record"
   generate_tfrecord.main()

def deleteModelCheckpoints():
   for filename in os.listdir("training/"):
      if "model" in filename or "checkpoint" in filename or "events" in filename:
         print("Deleting " + filename + "...")
         os.remove("training/" + filename)

def generateModel():
   print("Generating model for inference_graph...")
   export_tflite_ssd_graph.FLAGS.pipeline_config_path_export = "training/ssd_mobilenet_v1_pets.config"
   export_tflite_ssd_graph.FLAGS.trained_checkpoint_prefix = "training/model.ckpt-" + findLatestModel()
   export_tflite_ssd_graph.FLAGS.output_directory = "inference_graph"
   export_tflite_ssd_graph.main()

def findLatestModel():
   print("Searching for latest model...")
   latestModel = "0"
   for filename in os.listdir("training/"):
      if "model" in filename:
         if int(latestModel) < int(((filename.split("-")[1])).split(".")[0]):
            latestModel = ((filename.split("-")[1])).split(".")[0]
   print("Latest model found: " + latestModel)
   return latestModel

def convertToTFlite():
   print("converting inference graph to tflite file...")
   x = datetime.datetime.now()
   out_name = "tfliteModels/" + x.strftime("%Y") + x.strftime("%m") + x.strftime("%d") + ".tflite" 
   graph_def_file = "inference_graph/tflite_graph.pb"
   input_arrays = ["normalized_input_image_tensor"]
   output_arrays = ["TFLite_Detection_PostProcess","TFLite_Detection_PostProcess:1","TFLite_Detection_PostProcess:2","TFLite_Detection_PostProcess:3"]
   input_shapes = {"normalized_input_image_tensor" : [1, 300, 300, 3]}
   converter = tf.lite.TFLiteConverter.from_frozen_graph(graph_def_file, input_arrays, output_arrays, input_shapes)
   converter.inference_type = lite_constants.FLOAT
   converter.output_format = lite_constants.TFLITE
   converter.allow_custom_ops = True
   tflite_model = converter.convert()
   open(out_name, "wb").write(tflite_model)
   return out_name

def uploadToFirebase(out_name):
   print("Uploading tflite model to Firebase Storage...")
   storage.child(out_name).put(out_name)
   print("Model successfully upload to Firebase")

sortDownloaded()
addInfoToXml()
xml_to_csv.main()
generateTrainRecords()
generateTestRecords()
deleteModelCheckpoints()
train.main()
generateModel()
out_name = convertToTFlite()
uploadToFirebase(out_name)