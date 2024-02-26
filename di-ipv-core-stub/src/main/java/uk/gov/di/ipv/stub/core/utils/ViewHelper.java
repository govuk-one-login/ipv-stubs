package uk.gov.di.ipv.stub.core.utils;

import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.Map;

public class ViewHelper {
    private static final MustacheTemplateEngine TEMPLATE_ENGINE = new MustacheTemplateEngine();

    public static String render(Map<String, Object> model, String templatePath) {
        return TEMPLATE_ENGINE.render(new ModelAndView(model, templatePath));
    }
}
