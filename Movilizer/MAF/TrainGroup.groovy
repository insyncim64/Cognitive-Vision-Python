import com.movilizer.maf.bl.MAFDataManager
import com.movilizer.maf.bo.mappings.container.MAFUploadDataContainer
import com.movilizer.maf.scripting.MAFEventContext
import com.movilizer.maf.scripting.access.MAFNotificationGateway
import com.movilizer.FaceRecognitionManager

MAFNotificationGateway notificationManager = mafContext.getNotificationManager()
FaceRecognitionManager manager = new FaceRecognitionManager(mafContext)
def result = manager.trainGroup()
notificationManager.writeInfo("Train group: " + String.valueOf(result))