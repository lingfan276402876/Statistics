package uc.game.statistics.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class SummaryCubeConfig {
	private static Map<String, Map<String, SummaryCube>> map = new HashMap<String, Map<String, SummaryCube>>();
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
		Document document = reader.read(Thread.currentThread().getContextClassLoader().getResource("summaryCubes.xml"));
		Element root = document.getRootElement();
		List<Element> applications = root.elements();
		SummaryCubeConfig config = new SummaryCubeConfig();
		for (Element application : applications) {
			Map<String, SummaryCube> cubeMap = new HashMap<String, SummaryCube>();
			String applicationName = application.attributeValue("applicationName");
			List<Element> summaryCubes = application.elements();
			for (Element element : summaryCubes) {
				SummaryCube cube = config.new SummaryCube();
				cube.setSummaryCubeName(element.attributeValue("summaryCubeName"));
				cube.setPvName(element.attributeValue("pvName"));
				cube.setUvName(element.attributeValue("uvName"));
				cube.setRefCube(element.attributeValue("refCube"));
				List<Element> dimensions = element.element("dimensions").elements();
				for (Element dimension_Element : dimensions) {
					cube.getDimensions().add(config.new CubeDimension(dimension_Element.attributeValue("cubeDimension")));
				}
				cubeMap.put(cube.getSummaryCubeName(), cube);
			}
			map.put(applicationName, cubeMap);
		}
	}

	public class SummaryCube {
		private String summaryCubeName;
		private String uvName;
		private String pvName;
		private String refCube;
		private List<CubeDimension> dimensions = new ArrayList<CubeDimension>();

		public String getSummaryCubeName() {
			return summaryCubeName;
		}

		public void setSummaryCubeName(String summaryCubeName) {
			this.summaryCubeName = summaryCubeName;
		}

		public String getUvName() {
			return uvName;
		}

		public void setUvName(String uvName) {
			this.uvName = uvName;
		}

		public String getPvName() {
			return pvName;
		}

		public void setPvName(String pvName) {
			this.pvName = pvName;
		}

		public List<CubeDimension> getDimensions() {
			return dimensions;
		}

		public void setDimensions(List<CubeDimension> dimensions) {
			this.dimensions = dimensions;
		}

		public String getRefCube() {
			return refCube;
		}

		public void setRefCube(String refCube) {
			this.refCube = refCube;
		}
		
	}

	/**
	 * 维度
	 */
	public class CubeDimension {
		private String dimensionName;

		public CubeDimension(String dimensionName) {
			super();
			this.dimensionName = dimensionName;
		}

		public String getDimensionName() {
			return dimensionName;
		}

		public void setDimensionName(String dimensionName) {
			this.dimensionName = dimensionName;
		}
	}

	public static SummaryCube getCube(String applicationName, String logName) {
		return map.get(applicationName).get(logName);
	}

	
	public static List<SummaryCube> getSummaryCubesByCubeName(String applicationName, String cubeName) {
		List<SummaryCube> summaryCubes = new ArrayList<SummaryCube>();
		Map<String, SummaryCube> sumaryCubeMap = map.get(applicationName);
		Collection<SummaryCube> collection = sumaryCubeMap.values();
		for (SummaryCube summaryCube : collection) {
           if(summaryCube.getRefCube().equals(cubeName))
           {
        	   summaryCubes.add(summaryCube);
           }
		}
		return summaryCubes;
	}
	
	
	public static String getTableName(String applicationName, String logName,String time) {
		return applicationName + "_" + logName + "_summaryCube_"+time;
	}
	public static String getWholeTableName(String applicationName, String logName) {
		return applicationName + "_" + logName + "_summaryCube_whole";
	}
}
