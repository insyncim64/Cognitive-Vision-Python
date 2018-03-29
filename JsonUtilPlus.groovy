package com.movilizer;

import com.movilizer.maf.bo.json.MAFJsonElement;
import com.movilizer.maf.scripting.access.MAFNotificationGateway;

public class JsonUtilPlus {
  	
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