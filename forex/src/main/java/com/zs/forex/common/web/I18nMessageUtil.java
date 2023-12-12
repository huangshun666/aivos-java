package com.zs.forex.common.web;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

public class I18nMessageUtil {
    private static MessageSourceAccessor accessor;


    private static final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    private static void initMessageSourceAccessor(String language) throws IOException {
        Resource resource = resourcePatternResolver.getResource("classpath:i18n/" + language + ".properties");
        String fileName = resource.getURL().toString();
        int lastIndex = fileName.lastIndexOf(".");
        String baseName = fileName.substring(0, lastIndex);
        ReloadableResourceBundleMessageSource reloadableResourceBundleMessageSource = new ReloadableResourceBundleMessageSource();
        reloadableResourceBundleMessageSource.setBasename(baseName);
        reloadableResourceBundleMessageSource.setCacheSeconds(5);
        reloadableResourceBundleMessageSource.setDefaultEncoding("UTF-8");
        accessor = new MessageSourceAccessor(reloadableResourceBundleMessageSource);
    }

    public static String getMessage(String language, String message, String defaultMessage) {
        try {
            initMessageSourceAccessor(language);
        } catch (IOException e) {
            return "system busy";
        }
        return accessor.getMessage(message, defaultMessage);
    }

    public static String getMessage(BaseErrorInfoInterface baseErrorInfoInterface) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        return I18nMessageUtil.getMessage((requestAttributes == null ||
                        requestAttributes.getRequest().getHeader("lang") == null) ? "en" :
                        requestAttributes.getRequest().getHeader("lang"), baseErrorInfoInterface.getResultCode(),
                baseErrorInfoInterface.getResultMsg());


    }


}
