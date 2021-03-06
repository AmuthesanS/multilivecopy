package amu.aem.multilivecopy.core.wcmcommand;

import com.day.cq.commons.servlets.HtmlStatusResponseHelper;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.commands.WCMCommand;
import com.day.cq.wcm.api.commands.WCMCommandContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.apache.sling.api.servlets.HttpConstants.METHOD_POST;

@Component(service = WCMCommand.class)
public class CreateMultiLiveCopy implements WCMCommand {

    private static final String CSV_PATH = "/tmp/multilivecopy/csvfile";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Reference
    private RequestResponseFactory requestResponseFactory;

    @Reference
    private SlingRequestProcessor slingRequestProcessor;

    @Override
    public String getCommandName() {
        return "createMultiLiveCopy";
    }

    @Override
    public HtmlResponse performCommand(WCMCommandContext wcmCommandContext, SlingHttpServletRequest slingHttpServletRequest, SlingHttpServletResponse slingHttpServletResponse, PageManager pageManager) {

        HtmlResponse response = null;

        Map<String, Object> params = new HashMap<>();

        for (Map.Entry<String, String[]> entry : slingHttpServletRequest.getParameterMap().entrySet()) {
            String k = entry.getKey();
            String[] v = entry.getValue();
            params.put(k, v);
        }

        ResourceResolver resourceResolver = slingHttpServletRequest.getResourceResolver();

        InputStream is = resourceResolver.resolve(CSV_PATH).adaptTo(InputStream.class);

        String destination = slingHttpServletRequest.getParameter("destPath");

        try {
            if (is != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(inputStreamReader);
                String line;
                String cvsSplitBy = ",";
                boolean isTitle = true;
                String[] csvTitle = null;
                while ((line = br.readLine()) != null) {
                    //addin test comment
                    String[] data = line.split(cvsSplitBy);
                    if (isTitle) {
                        csvTitle = data;
                        isTitle = false;
                    } else {
                        //title
                        params.put("title", data[0]);
                        //pagename
                        params.put("label", data[1]);
                        Resource destinationRes = resourceResolver.getResource(destination);
                        if ("y".equalsIgnoreCase(data[2])) {
                            Resource childRes = destinationRes.getChild(data[1]);
                            //delete the existing livecopy page
                            if (childRes != null) {
                                resourceResolver.delete(childRes);
                                resourceResolver.commit();
                            }
                        }
                        params.put("cmd", "createLiveCopy");
                        HttpServletRequest request = requestResponseFactory.createRequest(METHOD_POST, "/bin/wcmcommand", params);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        HttpServletResponse httpResponse = requestResponseFactory.createResponse(out);
                        slingRequestProcessor.processRequest(request, httpResponse, slingHttpServletRequest.getResourceResolver());
                        if (data.length > 3) {
                            Resource childRes = destinationRes.getChild(data[1]);
                            ModifiableValueMap childModValueMap = childRes.adaptTo(Page.class).getContentResource().adaptTo(ModifiableValueMap.class);
                            for (int i = 3; i < data.length; i++) {
                                childModValueMap.put(csvTitle[i], data[i]);
                            }
                            childModValueMap.put("cq:propertyInheritanceCancelled", Arrays.copyOfRange(csvTitle, 3, csvTitle.length));
                            resourceResolver.commit();
                        }
                    }
                }
            }
        } catch (IOException | ServletException e) {
            logger.error("error reading uploaded csv");
            response = HtmlStatusResponseHelper.createStatusResponse(false, "error reading uploaded csv", destination);
        }
        if (response == null) {
            response = HtmlStatusResponseHelper.createStatusResponse(true, "All live copies created successfully", destination);
        }
        return response;
    }
}
