package dev.vality.reporter.endpoint;

import dev.vality.reporter.ReportingSrv;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@WebServlet("/reports/new-proto")
public class ReportsNewProtoServlet extends GenericServlet {

    private final ReportingSrv.Iface requestHandler;
    private Servlet thriftServlet;

    @Autowired
    public ReportsNewProtoServlet(ReportingSrv.Iface requestHandler) {
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
