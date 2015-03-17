package uc.game.statistics.servlet;

import java.awt.Color;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.j2ee.servlets.ImageServlet;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;

import uc.game.statistics.config.ReportViewConfig.View;
import uc.game.statistics.data.DataRegister;
import uc.game.statistics.utils.TimeUtils;
import uc.game.statistics.view.ReportResp;
import ar.com.fdvs.dj.core.DJConstants;
import ar.com.fdvs.dj.core.DJServletHelper;
import ar.com.fdvs.dj.core.DynamicJasperHelper;
import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.core.layout.LayoutManager;
import ar.com.fdvs.dj.domain.DynamicReport;
import ar.com.fdvs.dj.domain.Style;
import ar.com.fdvs.dj.domain.builders.ColumnBuilder;
import ar.com.fdvs.dj.domain.builders.ColumnBuilderException;
import ar.com.fdvs.dj.domain.builders.DynamicReportBuilder;
import ar.com.fdvs.dj.domain.constants.Border;
import ar.com.fdvs.dj.domain.constants.Font;
import ar.com.fdvs.dj.domain.constants.HorizontalAlign;
import ar.com.fdvs.dj.domain.constants.Transparency;
import ar.com.fdvs.dj.output.ReportWriter;
import ar.com.fdvs.dj.output.ReportWriterFactory;
import ar.com.fdvs.dj.util.SortUtils;

@Controller
public class ReportServlet extends  HttpServlet{
	private static final long serialVersionUID = -4473529642106655614L;

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			generateReport(request, response);
		} catch (ColumnBuilderException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (JRException e) {
			e.printStackTrace();
		}
	}

	public void generateReport(HttpServletRequest request, HttpServletResponse response) throws ColumnBuilderException, ClassNotFoundException, JRException, IOException {
		String realPath = request.getSession().getServletContext().getRealPath("/");
		Map<String,String[]> parameters = request.getParameterMap();
		String applicationName="songshu";
		if(request.getParameter("applicationName")!=null)
		{
			applicationName = request.getParameter("applicationName");
		}
		String reportName = "songshu_new_keep10";
		if(StringUtils.isNotBlank(request.getParameter("reportName")))
		{
			reportName = request.getParameter("reportName");
		}
		String endTime=TimeUtils.formatDate(System.currentTimeMillis(),"yyyyMMdd");
		if(StringUtils.isNotBlank(request.getParameter("endTime")))
		{
			endTime = request.getParameter("endTime");
		}
		String beginTime=TimeUtils.getNewFormatDate(endTime,"yyyyMMdd", -30);
		if(StringUtils.isNotBlank(request.getParameter("beginTime")))
		{
			beginTime = request.getParameter("beginTime");
		}
		
		ReportResp reportResp = DataRegister.getReportData().findReportData(parameters,applicationName, reportName, beginTime,endTime);
		DynamicReport dr = createDynamicReport(realPath,reportResp);
		String imageServletUrl = "reports/image";
		LayoutManager layoutManager = new ClassicLayoutManager();
		JRDataSource ds = new JRBeanCollectionDataSource(getDummyCollectionSorted(dr.getColumns(),reportResp));
		Map<String,String> parameter = new HashMap<String, String>();	
		exportToHtml(request, response, dr, layoutManager, ds, parameter, parameter);
	}

	private DynamicReport createDynamicReport(String realPath, ReportResp reportResp) throws ColumnBuilderException, ClassNotFoundException {
		DynamicReportBuilder drb = new DynamicReportBuilder();
		Style detailStyle = new Style();
		detailStyle.setBorder(Border.THIN());
		detailStyle.setBorderColor(Color.BLACK);
		detailStyle.setStretchWithOverflow(false);
		Style headerStyle = new Style();
		headerStyle.setBorder(Border.THIN());
		headerStyle.setBackgroundColor(new Color(230, 230, 230));
		headerStyle.setTransparency(Transparency.OPAQUE);
		Style titleStyle = new Style();
		titleStyle.setFont(Font.VERDANA_BIG_BOLD);
		titleStyle.setHorizontalAlign(HorizontalAlign.CENTER);
		titleStyle.setBlankWhenNull(true);
		titleStyle.setOverridesExistingStyle(true);
		titleStyle.setStretchWithOverflow(false);
		Style subtitleStyle = new Style();
		Style amountStyle = new Style();
		amountStyle.setHorizontalAlign(HorizontalAlign.RIGHT);
		drb.setDetailHeight(17).setMargins(30, 20, 30, 15).setDefaultStyles(titleStyle, subtitleStyle, headerStyle, detailStyle);
		List<View> views = reportResp.getViewNames();
		try {
			for (int i = 0; i < views.size(); i++) {
				View view = views.get(i);
				if (view.getFieldName().equalsIgnoreCase("stat_time")) {
					ColumnBuilder columnBuilder = ColumnBuilder.getNew();
					columnBuilder.setColumnProperty("stat_time", String.class.getName());
					columnBuilder.setTitle(view.getViewName()).setFixedWidth(true).setWidth(120);
					drb.addColumn(columnBuilder.build());
					continue;
				}
				ColumnBuilder columnBuilder = ColumnBuilder.getNew();
				columnBuilder.setColumnProperty("filed" + (i + 1), String.class.getName());
				columnBuilder.setTitle(view.getViewName()).setWidth(120).setFixedWidth(true).setTruncateSuffix("...");
				drb.addColumn(columnBuilder.build());
			}
		} catch (ColumnBuilderException e) {
			e.printStackTrace();
		}
		drb = drb.setTitle(reportResp.getReportName()).setPrintBackgroundOnOddRows(true).setIgnorePagination(true);
		drb = drb.setLeftMargin(0).setRightMargin(0).setTopMargin(0).setBottomMargin(0);
		drb.setUseFullPageWidth(true);
		DynamicReport dr = drb.build();
		return dr;
	}

	public Collection getDummyCollectionSorted(List columnlist,ReportResp reportResp ) {
		Collection dummyCollection = reportResp.getViewValues();
		return SortUtils.sortCollection(dummyCollection, columnlist);

	}
	
	public void exportToHtml(HttpServletRequest request, 
			HttpServletResponse response, 
			DynamicReport dynamicReport, 
			LayoutManager layoutManager, 
			JRDataSource ds, 
			Map parameters, 
			Map exporterParams) throws JRException, IOException
	{
		if (parameters == null)
			parameters = new HashMap();
		if (exporterParams == null)
			exporterParams = new HashMap();

		JasperPrint _jasperPrint = DynamicJasperHelper.generateJasperPrint(dynamicReport, layoutManager, ds,parameters);
		final ReportWriter reportWriter = ReportWriterFactory.getInstance().getReportWriter(_jasperPrint, DJConstants.FORMAT_HTML, parameters);
        JRExporter exporter = reportWriter.getExporter();
        exporter.setParameter(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, false);
        // Needed to support chart images:
        exporter.setParameter(JRExporterParameter.JASPER_PRINT, _jasperPrint);
        HttpSession session = request.getSession();
        session.setAttribute(ImageServlet.DEFAULT_JASPER_PRINT_SESSION_ATTRIBUTE, _jasperPrint);
		session.setAttribute("net.sf.jasperreports.j2ee.jasper_print", _jasperPrint);
        reportWriter.writeTo(response);
	}
}
