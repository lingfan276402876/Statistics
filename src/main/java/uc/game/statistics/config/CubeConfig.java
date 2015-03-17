package uc.game.statistics.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import uc.game.statistics.exception.GeneralLogicException;

public class CubeConfig {
	private static Map<String, Map<String, Cube>> map = new HashMap<String, Map<String, Cube>>();
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
		Document document = reader.read(Thread.currentThread().getContextClassLoader().getResource("cubes.xml"));
		Element root = document.getRootElement();
		List<Element> applications = root.elements();
		CubeConfig config = new CubeConfig();
		for (Element application : applications) {
			Map<String, Cube> cubeMap = new HashMap<String, Cube>();
			String applicationName = application.attributeValue("applicationName");
			List<Element> cubes = application.elements();
			for (Element element : cubes) {
				Cube cube = config.new Cube();
				cube.setCubeName(element.attributeValue("cubeName"));
				cube.setRefLog(element.attributeValue("refLog"));
				List<Element> dimensions = element.element("dimensions").elements();
				for (Element dimension_Element : dimensions) {
					cube.getDimensions().add(new CubeConfig().new Dimension(dimension_Element.attributeValue("fieldName"), dimension_Element.attributeValue("dimensionName")));
				}
				List<Element> kpis = element.element("kpis").elements();
				for (Element kpis_Element : kpis) {
					cube.getKpis().add(config.new KPI(kpis_Element.attributeValue("kpiName"), kpis_Element.attributeValue("fieldName"), kpis_Element.attributeValue("operationType")));
				}
				cubeMap.put(cube.getCubeName(), cube);
			}
			map.put(applicationName, cubeMap);
		}
	}

	public class Cube {
		private String cubeName;
		private String refLog;
		private List<Dimension> dimensions = new ArrayList<CubeConfig.Dimension>();;
		private List<KPI> kpis = new ArrayList<CubeConfig.KPI>();

		public String getCubeName() {
			return cubeName;
		}

		public void setCubeName(String cubeName) {
			this.cubeName = cubeName;
		}

		public List<Dimension> getDimensions() {
			return dimensions;
		}

		public void setDimensions(List<Dimension> dimensions) {
			this.dimensions = dimensions;
		}

		public List<KPI> getKpis() {
			return kpis;
		}

		public void setKpis(List<KPI> kpis) {
			this.kpis = kpis;
		}

		public String getRefLog() {
			return refLog;
		}

		public void setRefLog(String refLog) {
			this.refLog = refLog;
		}

		public String getFieldNameByDimensionName(String dimensionName) {
			for (Dimension dimension : this.dimensions) {
				if (dimension.getDimensionName().equals(dimensionName))
					return dimension.getFieldName();
			}
			throw new GeneralLogicException("总表维度" + dimensionName + "定义错误，必须和统计类型的维度定义一致");
		}

		public KPI getKpiByKpiName(String kpiName) {
			for (KPI kpi : this.kpis) {
				if (kpi.getKpiName().equals(kpiName))
					return kpi;
			}
			throw new GeneralLogicException("指标" + kpiName + "定义错误，必须和统计类型的维度定义一致");
		}

		/**
		 * 总表的uv字段
		 * 
		 * @param uvName
		 * @return
		 */
		public String getUniKey(String uvName) {
			String uniKey = null;
			for (KPI kpi : this.kpis) {
				if (kpi.getOperationType().equals(OperationType.UNIQUE) && kpi.getKpiName().equals(uvName)) {
					uniKey = kpi.getFieldName();
				}
			}
			return uniKey;
		}
	}

	/**
	 * 维度
	 */
	public class Dimension {
		private String fieldName;
		private String dimensionName;

		public Dimension(String fieldName, String dimensionName) {
			super();
			this.fieldName = fieldName;
			this.dimensionName = dimensionName;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		public String getDimensionName() {
			return dimensionName;
		}

		public void setDimensionName(String dimensionName) {
			this.dimensionName = dimensionName;
		}
	}

	/**
	 * 指标
	 */
	public class KPI {
		private String kpiName;
		private String fieldName;
		private OperationType operationType;

		public KPI(String kpiName, String fieldName, String operationType) {
			super();
			this.kpiName = kpiName;
			this.fieldName = fieldName;
			this.operationType = OperationType.valueOf(operationType);
		}

		public String getKpiName() {
			return kpiName;
		}

		public void setKpiName(String kpiName) {
			this.kpiName = kpiName;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		public OperationType getOperationType() {
			return operationType;
		}

		public void setOperationType(OperationType operationType) {
			this.operationType = operationType;
		}

	}

	public static Cube getCube(String applicationName, String cubeName) {
		return map.get(applicationName).get(cubeName);
	}

	public static List<Cube> getCubesByLogName(String applicationName, String logName) {
		List<Cube> cubes = new ArrayList<CubeConfig.Cube>();
		Map<String, Cube> cubeMap = map.get(applicationName);
		Collection<Cube> collection = cubeMap.values();
		for (Cube cube : collection) {
           if(cube.getRefLog().equals(logName))
           {
        	   cubes.add(cube);
           }
		}
		return cubes;
	}

	public static String getTableName(String applicationName, String logName, String time) {
		return applicationName + "_" + logName + "_cube_" + time;
	}
}
