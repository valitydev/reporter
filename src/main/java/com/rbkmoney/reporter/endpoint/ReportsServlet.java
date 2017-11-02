package com.rbkmoney.reporter.endpoint;

import com.rbkmoney.damsel.reports.ReportingSrv;
import com.rbkmoney.woody.thrift.impl.http.THServiceBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

/**
 * Created by tolkonepiu on 18/07/2017.
 */
@WebServlet("/reports")
public class ReportsServlet extends GenericServlet {

    private Servlet thriftServlet;

    private final ReportingSrv.Iface requestHandler;

    @Autowired
    public ReportsServlet(ReportingSrv.Iface requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        thriftServlet = new THServiceBuilder()
                .build(ReportingSrv.Iface.class, requestHandler);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        thriftServlet.service(req, res);
    }

}
