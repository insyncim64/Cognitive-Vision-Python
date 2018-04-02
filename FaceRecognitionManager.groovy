package com.movilizer

import com.movilizer.maf.bo.json.MAFJsonElement
import com.movilizer.maf.scripting.MAFContext
import com.movilizer.maf.scripting.access.MAFConnectorGateway

class FaceRecognitionManager {
	static final String KEY = "df1ba9afeb6240a7a937101115ba10b2"
	static final String IDENTIFY_URL = 'https://api.cognitive.azure.cn/face/v1.0/identify'
	static final String TRAIN_URL = 'https://api.cognitive.azure.cn/face/v1.0/persongroups/{personGroupId}/train'
	static final String ADD_FACE_URL = 'https://api.cognitive.azure.cn/face/v1.0/persongroups/{personGroupId}/persons/{personId}/persistedFaces'
	static final String ADD_PERSON_URL = 'https://api.cognitive.azure.cn/face/v1.0/persongroups/{personGroupId}/persons'
	static final String DETECT_URL = 'https://api.cognitive.azure.cn/face/v1.0/detect?returnFaceId=true'
	static final String VERIFY_URL = 'https://api.cognitive.azure.cn/face/v1.0/verify'
	static final String MD_POOL = "Person"
	static final String GROUP = "ALL"
	static final long DELAY     = 4L * 7L * 24L * 60L * 60L * 1000L //weeks * days * h * min * sec * millisec
	static final String FACE_GROUP = "test"

	private MAFContext context
	private MAFConnectorGateway connector
	private MAFNotificationGateway notificationGateway
	private MAFLifecycleGateway mds
	private long systemId

	/**
	 * Required constructor by the MAF sandbox
	 */
	FaceRecognitionManager() {}

	/**
	 * Required constructor by the MAF sandbox
	 */
	FaceRecognitionManager(MAFContext context) {
		this.context = context
		this.notificationGateway = context.getNotificationManager()
		this.connector = context.getConnectorManager()
		this.mds = context.getMDSLifecycleManager()
		this.systemId = context.getSystemID()
	}

	private HashMap<String, String> getFileUploadHeaders() {
		HashMap<String, String> properties = new HashMap<>()
		properties.put('Ocp-Apim-Subscription-Key', KEY)
		properties.put('Content-Type', 'application/octet-stream')
		return properties
	}

	private HashMap<String, String> getJSONHeaders() {
		HashMap<String, String> properties = new HashMap<>()
		properties.put('Ocp-Apim-Subscription-Key', KEY)
		properties.put('Content-Type', 'application/json')
		return properties
	}

	private boolean addPersonEntry(String name, String personId) {
		def systemTime = String.valueOf(System.currentTimeMillis())
		Hashtable mapping = new Hashtable()
		mapping.put("personId", personId)
		mapping.put("name", name)
		mds.sendMasterdata(
                  this.systemId,
                  MD_POOL,
                  GROUP,
                  name,
                  personId,
                  name,
                  "",
                  "",
                  Long.parseLong(systemTime),
                  null,
                  null,
                  new Date(Long.parseLong(systemTime) + DELAY),
                  mapping)

		return true
	}

	private String getPersonId(String name) {
		def key = new MAFMasterdataKey(this.systemId, MD_POOL, name)
		MAFMasterdataEntry entry = mds.loadMasterdata(key)
		def personId = entry.getDescription()
		return personId
	}

	public String identifyFace(byte[] file) {
		def faceId = detectFace(file)
		if(faceId == null)
			return false

		//def list = new ArrayList()
		//list.add(faceId)

		def faceIds = "[\"" + faceId + "\"]"
		def url = VERIFY_URL
		def json = new HashMap()
		json.put("faceIds", faceIds)
		json.put("personGroupId", FACE_GROUP)
		json.put("maxNumOfCandidatesReturned", 1)
    	json.put("confidenceThreshold", 0.5)

		def result = sendJSONRequest(url, "post", json)
		if(result != null && result.size() == 1 && result["0"]["candidates"].size() == 1) {
			return result["0"]["candidates"]["0"]["personId"]
		}
		return ""
	}

	/**
	* Verify if the given face belongs to the given person
	* @param byte array of the photo
	*/
	public boolean verifyFace(byte[] file, String name) {
		def faceId = detectFace(file)
		if(faceId == null)
			return false

		def personId = getPersonId(name)
		if(personId == null)
			return false
		
		def url = VERIFY_URL
		def json = new HashMap()
		json.put("faceId", faceId)
		json.put("personId", personId)
		json.put("personGroupId", FACE_GROUP)

		def result = sendJSONRequest(url, "post", json)
		if(result != null && result.get("isIdentical")) {
			return true
		}
		return false
	}

	/**
	* Detect if there is a face in the given photo
	* @param byte array of the photo
	* @return return the only face id, if no or more than one face, it will be empty
	*/
	public String detectFace(byte[] file) {
		def url = DETECT_URL
		def result = uploadFile(url, file)
		if(result != null) {
			if(result.keySet().size() == 0 || result.keySet().size() > 1)
				return null
			return result['0']['faceId']
		}
		return null
	}

	/**
	* Create a person by giving his/her name
	* @param name or id of this person
	* @return person id which is created by Azure
	*/
	public String createPerson(String name) {
		def url = ADD_PERSON_URL.replace("{personGroupId}", FACE_GROUP)
		def json = new HashMap()
		json.put("name", name)
		def result = sendJSONRequest(url, "post", json)
		if(result != null) {
			def personId = result.get("personId")
			if(personId != null) 
				addPersonEntry(name, personId)
			return personId
		}
		return null
	}

	/**
	* Add face to the group
	* @param personId
	* @return successful or not
	*/
	public boolean addFace(byte[] file, String name) {
		def personId = getPersonId(name)
		def url = ADD_FACE_URL.replace("{personGroupId}", FACE_GROUP)
		url = url.replace("{personId}", personId)
		return uploadFile(url, file) != null
	}

	/**
	* Train the whole group
	* @return successful or not
	*/
	public boolean trainGroup() {
		def url = TRAIN_URL.replace("{personGroupId}", FACE_GROUP)
		return sendJSONRequest(url, 'post', null) != null
	}

	/**
	 * Uploads a file to the service
	 * @param fileName to use in the uploading process
	 * @param file data in a byte array
	 * @return the url of the file just uploaded (empty if errors occurs during upload)
	 */
	Map<String, Object> sendJSONRequest(url, method, json) {
		HashMap<String, String> requestHeaders = getJSONHeaders()
		try {
			notificationGateway.writeDebug("Sending JSON request: " + method, json)
			def result = this.connector.doRESTCall(url, json, 'JSON', method, 'UTF-8', requestHeaders)
			notificationGateway.writeDebug("Send JSON successful", result)
			return result
		} catch(e) {
			notificationGateway.writeError('Upload exception', e)
		}
		return null
	}


	/**
	 * Uploads a file to the service
	 * @param fileName to use in the uploading process
	 * @param file data in a byte array
	 * @return the url of the file just uploaded (empty if errors occurs during upload)
	 */
	Map<String, Object> uploadFile(url, byte[] file) {
		HashMap<String, String> requestHeaders = getFileUploadHeaders()
		try {
			byte[] result = (byte[]) this.connector.doRESTCall(url, file, 'BYTES', 'POST', 'UTF-8', requestHeaders)
			if(result.length > 0) {
				String stringResult = new String(result, 'UTF-8')
				notificationGateway.writeDebug("Upload file successful", stringResult)
				MAFJsonElement jsonElement = this.context.parseJSON(stringResult)
				Map resultMap = jsonToMap(jsonElement, this.context.getNotificationManager())
				notificationGateway.writeDebug("Parse successful", resultMap)
				return resultMap
			} else {
				notificationGateway.writeWarning("No response back", null)
			}
		} catch(e) {
			notificationGateway.writeError('Upload exception', e)
		}
		return null
	}

	public static Map<String, Object> jsonToMap(MAFJsonElement json, MAFNotificationGateway notification) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		if(!json.isNull()) {
			if(json.isListType()) {
				retMap = toList(json, notification);
			} else if(json.isMapType()) {
				retMap = toMap(json, notification);
			}
		}
		return retMap;
	}

	public static Map<String, Object> toMap(MAFJsonElement object, MAFNotificationGateway notification) {
        Map<String, Object> result = object.asMap();
		Map<String, Object> map = new HashMap<String, Object>();
        Iterator<String> keysItr = result.keySet().iterator();
		while(keysItr.hasNext()) {
			String key = keysItr.next();
			Object value = result.get(key);
			Object valueResult = null;
			if(value.isListType()) {
				valueResult = toList(value, notification);
			} else if(value.isMapType()) {
				valueResult = toMap(value, notification);
			} else if(value.isPrimitiveType()) {
				valueResult = value.asString()
			}
			map.put(key, valueResult)
		}
		return map;
	}

	public static Map<String, Object> toList(MAFJsonElement object, MAFNotificationGateway notification) {
        List<Object> result = object.asList();
		Map<String, Object> map = new HashMap<String, Object>();
        for(int i = 0; i < result.size(); i++) {
			Object value = result.get(i)
			Object valueResult = null;
			if(value.isListType()) {
				valueResult = toList(value, notification);
			} else if(value.isMapType()) {
				valueResult = toMap(value, notification);
			} else if(value.isPrimitiveType()) {
				valueResult = value.asString()
			}
			map.put(String.valueOf(i), valueResult)
		}
		return map
	}
}