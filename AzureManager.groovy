package com.movilizer

import com.movilizer.maf.bo.json.MAFJsonElement
import com.movilizer.maf.scripting.MAFContext
import com.movilizer.maf.scripting.access.MAFConnectorGateway

class AzureManager {
	static final String KEY = "2e91458c4db94345899fb3669f1a93a2"
	static final String BLOB_STORAGE_URL = 'https://api.cognitive.azure.cn/vision/v1.0/analyze'
	private MAFContext context
	private MAFConnectorGateway connector
	private Logger logger

	/**
	 * Required constructor by the MAF sandbox
	 */
	AzureManager() {}

	/**
	 * Required constructor by the MAF sandbox
	 */
	AzureManager(MAFContext context) {
		this.context = context
		this.logger = new Logger(context, AzureManager.class)
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
	String uploadFile(byte[] file) {
		HashMap<String, String> requestHeaders = getUploadHeaders()
		String attachmentUrl = "${BLOB_STORAGE_URL}?visualFeatures=Color%2CCategories"
		try {
			byte[] result = (byte[]) this.connector.doRESTCall(attachmentUrl, file, 'BYTES', 'POST', 'UTF-8', requestHeaders)
			if(result.length > 0) {
				String stringResult = new String(result, 'UTF-8')
				this.logger.info("Upload file successful", stringResult)
				return stringResult
			} else {
				this.logger.info("No response back", null)
			}
		} catch(e) {
			this.logger.error('Upload blob exception: ' + e.getMessage(), e)
		}
		return ""
	}
}