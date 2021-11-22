package uk.gov.di.ipv.stub.cred.utils;

import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.Map;
import java.util.Set;

public class ViewHelper {
    public static String render(Map model, String templatePath) {
        return new MustacheTemplateEngine().render(new ModelAndView(model, templatePath));
    }

    public static String renderSet(Set set, String templatePath) {
        return new MustacheTemplateEngine().render(new ModelAndView(set, templatePath));
    }
}
