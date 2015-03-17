package uc.game.statistics.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class LogConfig {
	public static Map<String, Map<String, Log>> map = new HashMap<String, Map<String, Log>>();
	static {
		try {
			initialize();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	public static void reload()
	{
		try {
			initialize();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@SuppressWarnings("unchecked")
	private static void initialize() throws Exception {
		SAXReader reader = new SAXReader();
		Document document = reader.read(Thread.currentThread().getContextClassLoader().getResource("logs.xml"));
		Element root = document.getRootElement();
		List<Element> applications = root.elements();
		LogConfig config = new LogConfig();
		for (Element element : applications) {
			Map<String, Log> logMap = new HashMap<String, LogConfig.Log>();
			String applicationName = element.attributeValue("applicationName");
			List<Element> logs = element.elements();
			for (Element lo : logs) {
				Log log = config.new Log();
				log.setLogName(lo.attributeValue("logName"));
				List<Element> fields = lo.elements();
				for (Element field : fields) {
					log.getFields().add(config.new Field(field.attributeValue("fieldName"), field.attributeValue("dataType")));
				}
				logMap.put(log.getLogName(), log);
			}
			map.put(applicationName, logMap);
		}

	}

	public class Log {
		private String logName;
		private List<Field> fields = new ArrayList<LogConfig.Field>();

		public String getLogName() {
			return logName;
		}

		public void setLogName(String logName) {
			this.logName = logName;
		}

		public List<Field> getFields() {
			return fields;
		}

		public void setFields(List<Field> fields) {
			this.fields = fields;
		}

		public DataType getDataType(String filedName) {
			for (Field field : fields) {
				if (field.getName().equals(filedName)) {
					return field.getDataType();
				}
			}
			return null;
		}
	}

	public class Field {
		private String name;
		private DataType dataType;

		public Field(String name, String dataType) {
			this.name = name;
			this.dataType = DataType.valueOf(dataType);
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public DataType getDataType() {
			return dataType;
		}

		public void setDataType(DataType dataType) {
			this.dataType = dataType;
		}
	}

	public static Log getLog(String applicationName,String logName) {
		return map.get(applicationName).get(logName);
	}

	public static String getTableName(String applicationName, String logName,String time) {
		return applicationName + "_" + logName + "_log_"+time;
	}
}
