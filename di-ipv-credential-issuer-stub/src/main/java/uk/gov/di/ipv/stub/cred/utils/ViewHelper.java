package uk.gov.di.ipv.stub.cred.utils;

import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.Map;

public class ViewHelper {
    private final MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

    public String render(Map<String, Object> model, String templatePath) {
        return templateEngine.render(new ModelAndView(model, templatePath));
    }
}
