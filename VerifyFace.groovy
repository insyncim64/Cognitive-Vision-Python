import com.movilizer.maf.bl.MAFDataManager
import com.movilizer.maf.bo.mappings.container.MAFUploadDataContainer
import com.movilizer.maf.scripting.MAFEventContext
import com.movilizer.maf.scripting.access.MAFNotificationGateway
import com.movilizer.FaceRecognitionManager

FaceRecognitionManager manager = new FaceRecognitionManager(mafContext)
MAFUploadDataContainer dataContainer = mafContext.getOnlineContainers().get(0)
MAFNotificationGateway notificationManager = mafContext.getNotificationManager()
String key = dataContainer.getKey()
notificationManager.writeDebug("Upload container received $key", null)
byte[] file = (byte[]) dataContainer.getObject("Base64File")
String name = (String)dataContainer.getObject("name")

def result = manager.verifyFace(file, name)
notificationManager.writeInfo("Verify face: " + String.valueOf(result), null)
Hashtable onlineContainerReply = new Hashtable()
onlineContainerReply.put("result", result)
mafContext.addOnlineContainerReply("result", onlineContainerReply)
