package com.movilizer

import com.movilizer.maf.bo.json.MAFJsonElement
import com.movilizer.maf.scripting.MAFContext
import com.movilizer.maf.scripting.access.MAFConnectorGateway

class AzureManager {
	static final String KEY = "558da1e602304d0ca7dcdb977889f079"
	static final String BLOB_STORAGE_URL = 'https://api.cognitive.azure.cn/vision/v1.0/ocr?language=zh-Hans&detectOrientation=true'
	private MAFContext context
	private MAFConnectorGateway connector
	private MAFNotificationGateway notificationGateway

	/**
	 * Required constructor by the MAF sandbox
	 */
	AzureManager() {}

	/**
	 * Required constructor by the MAF sandbox
	 */
	AzureManager(MAFContext context) {
		this.context = context
		this.notificationGateway = context.getNotificationManager()
		this.connector = context.getConnectorManager()
	}

	private HashMap<String, String> getUploadHeaders() {
		HashMap<String, String> properties = new HashMap<>()
		properties.put('Ocp-Apim-Subscription-Key', KEY)
		properties.put('Content-Type', 'application/octet-stream')
		return properties
	}

	/**
	 * Uploads a file to the blob storage service
	 * @param fileName to use in the uploading process
	 * @param file data in a byte array
	 * @return the url of the file just uploaded (empty if errors occurs during upload)
	 */
	Map<String, Object> uploadFile(byte[] file) {
		HashMap<String, String> requestHeaders = getUploadHeaders()
		String attachmentUrl = "${BLOB_STORAGE_URL}?visualFeatures=Color%2CCategories"
		try {
			byte[] result = (byte[]) this.connector.doRESTCall(attachmentUrl, file, 'BYTES', 'POST', 'UTF-8', requestHeaders)
			if(result.length > 0) {
				String stringResult = new String(result, 'UTF-8')
				notificationGateway.writeInfo("Upload file successful", stringResult)
				MAFJsonElement jsonElement = this.context.parseJSON(stringResult)
      			Map resultMap = jsonToMap(jsonElement, this.context.getNotificationManager());
				notificationGateway.writeInfo("Parse successful", resultMap)
				Map<String, Object> lines = new HashMap<String, Object>()
				Map lineMap = resultMap["regions"]["0"]["lines"]
				Iterator<String> keysItr = lineMap.keySet().iterator()
				while(keysItr.hasNext()) {
					String key = keysItr.next();
					Map value = lineMap.get(key);
					String boundingBox = value["boundingBox"]
					Map words = value["words"]
					StringBuffer buffer = new StringBuffer()
					Iterator<String> wordItr = words.keySet().iterator()
					while(wordItr.hasNext()) {
						String wordKey = wordItr.next()
						buffer.append(words.get(wordKey))
					}
					lines.put(boundingBox, buffer.toString())
				}
				return lines
			} else {
				notificationGateway.writeInfo("No response back", null)
			}
		} catch(e) {
			notificationGateway.writeError('Upload blob exception: ' + e.getMessage(), e)
		}
		return ""
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