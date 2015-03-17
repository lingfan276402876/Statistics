package uc.game.statistics.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import uc.game.statistics.config.ReportViewConfig.View;
import uc.game.statistics.data.ReportData;
import uc.game.statistics.view.GeneralReport;
import uc.game.statistics.view.ReportResp;
import ar.com.fdvs.dj.core.DynamicJasperHelper;
import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.domain.DynamicReport;
import ar.com.fdvs.dj.domain.builders.ColumnBuilderException;
import ar.com.fdvs.dj.domain.builders.FastReportBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
@Transactional
public class DataManagerTest {
	@Resource
	private DataManager dataManager;
	@Resource
	private ReportData reportData;
	@Resource
	private JdbcTemplate jdbcTemplate;

	@Test
	@Transactional(readOnly = false)
	@Rollback(value = false)
	public void readDataTest() {
		Map<String, String[]> parameters = new HashMap<String, String[]>();
		//parameters.put("os", "1");
		ReportResp reportResp = reportData.findReportData(parameters, "songshu", "songshu_flux", "20140403", "20140422");
		List<View> views = reportResp.getViewNames();
		FastReportBuilder drb = new FastReportBuilder();
		DynamicReport dr = null;
		try {
			for (int i = 0; i < views.size(); i++) {
				View view = views.get(i);
				drb = drb.addColumn(view.getViewName(),"filed"+(i+1), String.class.getName(), 30);
			}
		} catch (ColumnBuilderException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		dr = drb.build();
		Collection<GeneralReport> collection = reportResp.getViewValues();
		JRDataSource ds = new JRBeanCollectionDataSource(collection);
		JasperPrint jp = null;
		try {
			jp = DynamicJasperHelper.generateJasperPrint(dr, new ClassicLayoutManager(), ds);
			DynamicJasperHelper.generateJRXML(dr,new ClassicLayoutManager(), null, "UTF-8",System.getProperty("user.dir")+ "/target/reports/" + this.getClass().getName() + ".jrxml");
		} catch (JRException e) {
			e.printStackTrace();
		}
	
		JasperViewer.viewReport(jp);
	}
}
