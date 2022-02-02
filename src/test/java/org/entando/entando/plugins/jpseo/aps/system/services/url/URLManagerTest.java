/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.plugins.jpseo.aps.system.services.url;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.util.ApsProperties;
import org.entando.entando.plugins.jpseo.aps.system.services.page.PageMetatag;
import org.entando.entando.plugins.jpseo.aps.system.services.page.SeoPageMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class URLManagerTest {

    @InjectMocks
    private URLManager urlManager;
    
    @Mock
    private ConfigInterface configManager;
    @Mock
    private IPageManager pageManager;
    @Mock
    private ILangManager langManager;
    
    private IPage page;
    
    private MockHttpServletRequest request = new MockHttpServletRequest();
    
    @BeforeEach
    public void setUp() throws Exception {
        Lang defaultLang = mock(Lang.class);
        Mockito.lenient().when(defaultLang.getCode()).thenReturn("en");
        Mockito.lenient().when(langManager.getDefaultLang()).thenReturn(defaultLang);
        this.page = mock(IPage.class);
        SeoPageMetadata metadata = new SeoPageMetadata();
        ApsProperties friendlyCodes = new ApsProperties();
        friendlyCodes.put("it", new PageMetatag("it", "friendlyCode", "ita_friendly"));
        friendlyCodes.put("en", new PageMetatag("en", "friendlyCode", "en_friendly"));
        metadata.setFriendlyCodes(friendlyCodes);
        Mockito.lenient().when(page.getMetadata()).thenReturn(metadata);
        Mockito.lenient().when(pageManager.getOnlinePage(Mockito.anyString())).thenReturn(this.page);
        Mockito.lenient().when(page.getCode()).thenReturn("homepage");
        Mockito.lenient().when(configManager.getParam(SystemConstants.PAR_APPL_BASE_URL)).thenReturn("http://www.entando.com/Entando");
    }
    
    @Test
    void testStaticBaseUrl() throws Throwable {
        String expectedUrl = "http://www.entando.com/Entando/page/it/ita_friendly";
        RequestContext reqCtx = this.createRequestContext();
        Lang requestedLang = new Lang();
        requestedLang.setCode("it");
        requestedLang.setDescr("Italiano");
        Mockito.lenient().when(langManager.getLang("it")).thenReturn(requestedLang);
        PageURL pageURL = urlManager.createURL(reqCtx);
        pageURL.setLangCode("it");
        pageURL.setPageCode("homepage");
        String url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals(expectedUrl, url);
        
        pageURL.setBaseUrlMode("current");
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals(expectedUrl, url);
        
        pageURL.setBaseUrlMode("requestIfRelative");
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals(expectedUrl, url);
        
        Mockito.lenient().when(pageManager.getConfig(IPageManager.CONFIG_PARAM_BASE_URL)).thenReturn(IPageManager.CONFIG_PARAM_BASE_URL_FROM_REQUEST);
        pageURL.setBaseUrlMode(IPageManager.CONFIG_PARAM_BASE_URL_STATIC);
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals(expectedUrl, url);
    }
    
    @Test
    void testRelativeBaseUrl() throws Throwable {
        String expectedUrl = "/page/en/en_friendly";
        RequestContext reqCtx = this.createRequestContext();
        PageURL pageURL = urlManager.createURL(reqCtx);
        pageURL.setLangCode("en");
        pageURL.setPageCode("homepage");
        Mockito.lenient().when(pageManager.getConfig(IPageManager.CONFIG_PARAM_BASE_URL)).thenReturn(IPageManager.CONFIG_PARAM_BASE_URL_RELATIVE);
        
        String url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals(expectedUrl, url);
        
        pageURL.setBaseUrlMode("current");
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals(expectedUrl, url);
        
        pageURL.setBaseUrlMode(IPageManager.SPECIAL_PARAM_BASE_URL_REQUEST_IF_RELATIVE);
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals("http://www.entando.org/page/en/en_friendly", url);
        
        Mockito.lenient().when(pageManager.getConfig(IPageManager.CONFIG_PARAM_BASE_URL_CONTEXT)).thenReturn("true");
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals("http://www.entando.org/entando/page/en/en_friendly", url);
        
        request.addHeader("X-Forwarded-Proto", "https");
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals("https://www.entando.org/entando/page/en/en_friendly", url);
        
        pageURL.setBaseUrlMode("current");
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals("/entando" + expectedUrl, url);
        
        Mockito.lenient().when(pageManager.getConfig(IPageManager.CONFIG_PARAM_BASE_URL)).thenReturn(IPageManager.CONFIG_PARAM_BASE_URL_FROM_REQUEST);
        pageURL.setBaseUrlMode(IPageManager.CONFIG_PARAM_BASE_URL_RELATIVE);
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals("/entando" + expectedUrl, url);
        
        Mockito.lenient().when(pageManager.getConfig(IPageManager.CONFIG_PARAM_BASE_URL_CONTEXT)).thenReturn("false");
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals(expectedUrl, url);
    }
    
    @Test
    void testRequestBaseUrl() throws Throwable {
        String expectedUrl = "www.entando.org/page/en/en_friendly";
        String expectedUrlWithContext = "www.entando.org/entando/page/en/en_friendly";
        RequestContext reqCtx = this.createRequestContext();
        PageURL pageURL = urlManager.createURL(reqCtx);
        pageURL.setLangCode("en");
        pageURL.setPageCode("homepage");
        Mockito.lenient().when(pageManager.getConfig(IPageManager.CONFIG_PARAM_BASE_URL)).thenReturn(IPageManager.CONFIG_PARAM_BASE_URL_FROM_REQUEST);
        
        String url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals("http://"+expectedUrl, url);
        
        Mockito.lenient().when(pageManager.getConfig(IPageManager.CONFIG_PARAM_BASE_URL_CONTEXT)).thenReturn("true");
        
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals("http://"+expectedUrlWithContext, url);
        
        Mockito.lenient().when(pageManager.getConfig(IPageManager.CONFIG_PARAM_BASE_URL)).thenReturn(IPageManager.CONFIG_PARAM_BASE_URL_STATIC);
        pageURL.setBaseUrlMode(IPageManager.CONFIG_PARAM_BASE_URL_FROM_REQUEST);
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals("http://"+expectedUrlWithContext, url);
        
        Mockito.lenient().when(pageManager.getConfig(IPageManager.CONFIG_PARAM_BASE_URL_CONTEXT)).thenReturn("false");
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals("http://"+expectedUrl, url);
        
        request.addHeader("X-Forwarded-Proto", "https");
        url = this.urlManager.getURLString(pageURL, reqCtx);
        assertEquals("https://"+expectedUrl, url);
    }
    
    public RequestContext createRequestContext() {
        RequestContext reqCtx = new RequestContext();
        request.setScheme("http");
        request.setServerName("www.entando.org");
        request.addHeader("Host", "www.entando.org");
        request.setContextPath("/entando");
        request.setAttribute(RequestContext.REQCTX, reqCtx);
        MockHttpServletResponse response = new MockHttpServletResponse();
        reqCtx.setRequest(request);
        reqCtx.setResponse(response);
        Lang defaultLang = new Lang();
        defaultLang.setCode("en");
        defaultLang.setDescr("English");
        reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG, defaultLang);
        Mockito.lenient().when(langManager.getLang("en")).thenReturn(defaultLang);
        return reqCtx;
    }
    
    @Test
    void testGetUrl() throws Throwable {
        String expectedUrl = "www.entando.org/page/en/en_friendly";
        String expectedUrlWithContext = "www.entando.org/entando/page/en/en_friendly";
        RequestContext reqCtx = this.createRequestContext();
        Lang lang = new Lang();
        lang.setCode("en");
        lang.setDescr("English");
        Mockito.lenient().when(pageManager.getConfig(IPageManager.CONFIG_PARAM_BASE_URL)).thenReturn(IPageManager.CONFIG_PARAM_BASE_URL_FROM_REQUEST);
        String url = this.urlManager.createURL(this.page, lang, null, false, this.request);
        assertEquals("http://"+expectedUrl, url);
        Mockito.lenient().when(pageManager.getConfig(IPageManager.CONFIG_PARAM_BASE_URL_CONTEXT)).thenReturn("true");
        url = this.urlManager.createURL(this.page, lang, null, false, this.request);
        assertEquals("http://"+expectedUrlWithContext, url);
        request.addHeader("X-Forwarded-Proto", "https");
        url = this.urlManager.createURL(this.page, lang, null, false, this.request);
        assertEquals("https://"+expectedUrlWithContext, url);
    }
    
}
