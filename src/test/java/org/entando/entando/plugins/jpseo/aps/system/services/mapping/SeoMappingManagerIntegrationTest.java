/*
 * Copyright 2020-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.entando.entando.plugins.jpseo.aps.system.services.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.page.Page;
import com.agiletec.aps.system.services.page.PageMetadata;
import com.agiletec.aps.system.services.page.PageTestUtil;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.system.services.pagemodel.IPageModelManager;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import java.util.Date;
import java.util.Set;
import org.entando.entando.aps.system.services.widgettype.IWidgetTypeManager;
import org.entando.entando.plugins.jpseo.aps.system.JpseoSystemConstants;
import org.entando.entando.plugins.jpseo.aps.system.services.page.PageMetatag;
import org.entando.entando.plugins.jpseo.aps.system.services.page.SeoPageMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeoMappingManagerIntegrationTest extends BaseTestCase {
    
    private IContentManager contentManager;
    private IPageManager pageManager;
    private IPageModelManager pageModelManager;
    private ISeoMappingManager seoMappingManager;
    private IWidgetTypeManager widgetTypeManager;
    
    @Test
    void testCreateFriendlyCode_1() throws Exception {
        String contentId1 = null;
        Content content1 = null;
        String contentId2 = null;
        Content content2 = null;
        String contentId3 = null;
        Content content3 = null;
        try {
            content1 = this.contentManager.loadContent("EVN25", true);
            content1.setId(null);
            content1.setMainGroup(Group.FREE_GROUP_NAME);
            
            this.contentManager.insertOnLineContent(content1);
            synchronized (this) {
                this.wait(500);
            }
            super.waitNotifyingThread();
            contentId1 = content1.getId();
            String friendlyCode = this.seoMappingManager.getContentReference(contentId1, null);
            assertEquals("teatro_delle_meraviglie", friendlyCode);
            
            content2 = this.contentManager.loadContent("EVN25", true);
            content2.setId(null);
            content2.setMainGroup(Group.FREE_GROUP_NAME);
            this.contentManager.insertOnLineContent(content2);
            synchronized (this) {
                this.wait(500);
            }
            super.waitNotifyingThread();
            contentId2 = content2.getId();
            String friendlyCode2 = this.seoMappingManager.getContentReference(contentId2, null);
            assertEquals("teatro_delle_meraviglie_it_1", friendlyCode2);
            
            content3 = this.contentManager.loadContent("EVN25", true);
            content3.setId(null);
            content3.setMainGroup(Group.FREE_GROUP_NAME);
            this.contentManager.insertOnLineContent(content3);
            synchronized (this) {
                this.wait(500);
            }
            super.waitNotifyingThread();
            contentId3 = content3.getId();
            String friendlyCode3 = this.seoMappingManager.getContentReference(contentId3, null);
            assertEquals("teatro_delle_meraviglie_it_2", friendlyCode3);
            
            this.contentManager.removeOnLineContent(content1);
            synchronized (this) {
                this.wait(500);
            }
            friendlyCode = this.seoMappingManager.getContentReference(contentId1, null);
            assertNull(friendlyCode);
            
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != contentId1) {
                this.contentManager.removeOnLineContent(content1);
                this.contentManager.deleteContent(content1);
            }
            if (null != contentId2) {
                this.contentManager.removeOnLineContent(content2);
                this.contentManager.deleteContent(content2);
            }
            if (null != contentId3) {
                this.contentManager.removeOnLineContent(content3);
                this.contentManager.deleteContent(content3);
            }
        }
    }
    
    @Test
    void testCreateFriendlyCode_2() throws Exception {
        String contentId1 = null;
        Content content = null;
        try {
            content = this.contentManager.loadContent("EVN25", true);
            content.setId(null);
            content.setMainGroup(Group.FREE_GROUP_NAME);
            
            this.contentManager.insertOnLineContent(content);
            synchronized (this) {
                this.wait(500);
            }
            super.waitNotifyingThread();
            contentId1 = content.getId();
            String friendlyCode_1 = this.seoMappingManager.getContentReference(contentId1, null);
            assertEquals("teatro_delle_meraviglie", friendlyCode_1);
            
            TextAttribute titleAttribute = (TextAttribute) content.getAttributeByRole(JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE);
            assertNotNull(titleAttribute);
            assertEquals("TEATRO DELLE MERAVIGLIE", titleAttribute.getTextForLang("it"));
            titleAttribute.setText("Le meraviglie del teatro", "it");
            this.contentManager.insertOnLineContent(content);
            synchronized (this) {
                this.wait(500);
            }
            super.waitNotifyingThread();
            
            String friendlyCode_2 = this.seoMappingManager.getContentReference(contentId1, null);
            assertEquals(friendlyCode_1, friendlyCode_2);
            
            this.contentManager.removeOnLineContent(content);
            synchronized (this) {
                this.wait(500);
            }
            super.waitNotifyingThread();
            String friendlyCode_3 = this.seoMappingManager.getContentReference(contentId1, null);
            assertNull(friendlyCode_3);
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != contentId1) {
                this.contentManager.removeOnLineContent(content);
                this.contentManager.deleteContent(content);
            }
        }
    }
    
    @Test
    void testCreateFriendlyCode_3() throws Exception {
        String code1 = "test1";
        String code2 = "test2";
        try {
            this.addPage(code1, "service", "friendly1");
            this.addPage(code2, "service", "friendly2");
            synchronized (this) {
                this.wait(500);
            }
            super.waitNotifyingThread();
            FriendlyCodeVO friendlyCodeVO1 = this.seoMappingManager.getReference("friendly1");
            assertNotNull(friendlyCodeVO1);
            assertEquals(code1, friendlyCodeVO1.getPageCode());
            FriendlyCodeVO friendlyCodeVO2 = this.seoMappingManager.getReference("friendly2");
            assertNotNull(friendlyCodeVO2);
            assertEquals(code2, friendlyCodeVO2.getPageCode());
            
            IPage page = this.pageManager.getOnlinePage(code1);
            PageMetatag pageMetatag = new PageMetatag("it", "it", "friendly2_bis");
            ((SeoPageMetadata) page.getMetadata()).getFriendlyCodes().put("it", pageMetatag);
            this.pageManager.updatePage(page);
            this.pageManager.setPageOnline(code1);
            synchronized (this) {
                this.wait(500);
            }
            super.waitNotifyingThread();
            friendlyCodeVO1 = this.seoMappingManager.getReference("friendly1");
            assertNull(friendlyCodeVO1);
            friendlyCodeVO1 = this.seoMappingManager.getReference("friendly2_bis");
            assertNotNull(friendlyCodeVO1);
            assertEquals(code1, friendlyCodeVO1.getPageCode());
        } catch (Exception e) {
            throw e;
        } finally {
            this.pageManager.setPageOffline(code2);
            this.pageManager.deletePage(code2);
            this.pageManager.setPageOffline(code1);
            this.pageManager.deletePage(code1);
        }
    }
    
    private void addPage(String code, String parentCode, String friendlyCode) throws Exception {
        IPage parentPage = pageManager.getDraftPage(parentCode);
        String parentForNewPage = parentPage.getParentCode();
        PageModel pageModel = this.pageModelManager.getPageModel(parentPage.getMetadata().getModelCode());
        PageMetadata metadata = this.createSeoPageMetadata(pageModel,
                true, "pagina temporanea", null, null, false, null, null, friendlyCode);
        ApsProperties config = PageTestUtil.createProperties("actionPath", "/myJsp.jsp", "param1", "value1");
        Widget widgetToAdd = PageTestUtil.createWidget("formAction", config);
        Widget[] widgets = new Widget[pageModel.getFrames().length]; 
        widgets[0] = widgetToAdd;
        Page pageToAdd = PageTestUtil.createPage(code, parentForNewPage, "free", pageModel, metadata, widgets);
        this.pageManager.addPage(pageToAdd);
        this.pageManager.setPageOnline(code);
    }
    
	private SeoPageMetadata createSeoPageMetadata(PageModel pageModel, boolean showable, String defaultTitle, String mimeType,
			String charset, boolean useExtraTitles, Set<String> extraGroups, Date updatedAt, String friendlyCode) {
        SeoPageMetadata metadata = new SeoPageMetadata();
		metadata.setModelCode(pageModel.getCode());
        PageMetatag pageMetatag = new PageMetatag("it", "it", friendlyCode);
        metadata.getFriendlyCodes().put("it", pageMetatag);
		metadata.setShowable(showable);
		metadata.setTitle("it", defaultTitle);
		if (extraGroups != null) {
			metadata.setExtraGroups(extraGroups);
		}
		metadata.setMimeType(mimeType);
		metadata.setCharset(charset);
		metadata.setUseExtraTitles(useExtraTitles);
		metadata.setExtraGroups(extraGroups);
		metadata.setUpdatedAt(updatedAt);
		return metadata;
	}
    
    @BeforeEach
    private void init() throws Exception {
        try {
            this.contentManager = (IContentManager) this.getService(JacmsSystemConstants.CONTENT_MANAGER);
            this.seoMappingManager = (ISeoMappingManager) this.getService(JpseoSystemConstants.SEO_MAPPING_MANAGER);
            this.pageManager = (IPageManager) this.getService(SystemConstants.PAGE_MANAGER);
            this.pageModelManager = (IPageModelManager) this.getService(SystemConstants.PAGE_MODEL_MANAGER);
            this.widgetTypeManager = (IWidgetTypeManager) this.getService(SystemConstants.WIDGET_TYPE_MANAGER);
        } catch (Throwable t) {
            throw new Exception(t);
        }
    }
    
}
