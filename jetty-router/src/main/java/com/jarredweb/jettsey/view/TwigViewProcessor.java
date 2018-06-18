package com.jarredweb.jettsey.view;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Map;
import org.eclipse.jetty.server.Response;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.resource.reference.ResourceReference;

public class TwigViewProcessor implements ViewProcessor{
    
    protected final ViewConfiguration factory;

    public TwigViewProcessor(ViewConfiguration factory) {
        this.factory = factory;
    }
    
    @Override
    public void write(Response response, JtwigTemplate template, String view, String contentType, Map<String, Object> model) throws IOException {
        response.setContentType(contentType);
        JtwigModel viewModel = JtwigModel.newModel(model);
        template.render(viewModel, new PrintStream(response.getOutputStream(), true, "UTF-8"));
    }

    @Override
    public JtwigTemplate resolve(String templatePath, ResourceReference where) throws Exception {
        ResourceReference resource = new ResourceReference(where.getType(), Paths.get(templatePath).toFile().getAbsolutePath());
        JtwigTemplate jtwigTemplate = new JtwigTemplate(factory.getEnvironment(), resource);
        return jtwigTemplate;
    }
}