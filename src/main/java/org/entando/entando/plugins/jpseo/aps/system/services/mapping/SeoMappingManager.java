/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import com.agiletec.aps.system.common.AbstractService;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.ITextAttribute;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.page.events.PageChangedEvent;
import com.agiletec.aps.system.services.page.events.PageChangedObserver;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.event.PublicContentChangedEvent;
import com.agiletec.plugins.jacms.aps.system.services.content.event.PublicContentChangedObserver;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.plugins.jpseo.aps.system.JpseoSystemConstants;
import org.entando.entando.plugins.jpseo.aps.system.services.mapping.cache.ISeoMappingCacheWrapper;
import org.entando.entando.plugins.jpseo.aps.system.services.mapping.event.SeoChangedEvent;
import org.entando.entando.plugins.jpseo.aps.system.services.page.PageMetatag;
import org.entando.entando.plugins.jpseo.aps.system.services.page.SeoPageMetadata;
import org.entando.entando.plugins.jpseo.aps.util.FriendlyCodeGenerator;

/**
 * @author E.Santoboni, E.Mezzano
 */
public class SeoMappingManager extends AbstractService implements ISeoMappingManager, PageChangedObserver, PublicContentChangedObserver {

    private static final EntLogger logger =  EntLogFactory.getSanitizedLogger(SeoMappingManager.class);

    private ISeoMappingDAO seoMappingDAO;
    private ILangManager langManager;
    private IPageManager pageManager;
    private ISeoMappingCacheWrapper cacheWrapper;

    @Override
    public void init() throws Exception {
        this.getCacheWrapper().initCache(this.getPageManager(), this.getSeoMappingDAO(), true);
        logger.debug("{} ready. initialized",this.getClass().getName());
    }

    @Override
    protected void release() {
        this.getCacheWrapper().release();
        super.release();
    }

    @Override
    public void updateFromPageChanged(PageChangedEvent event) {
        IPage page = event.getPage();
        String eventType = event.getEventType();
        if (null == page || !(page.getMetadata() instanceof SeoPageMetadata) ||
                PageChangedEvent.EVENT_TYPE_JOIN_WIDGET.equals(eventType) ||
                PageChangedEvent.EVENT_TYPE_MOVE_WIDGET.equals(eventType) ||
                PageChangedEvent.EVENT_TYPE_REMOVE_WIDGET.equals(eventType)) {
            return;
        }
        SeoPageMetadata seoMetadata = (SeoPageMetadata) page.getMetadata();
        ApsProperties friendlyCodes = (event.getOperationCode() == PageChangedEvent.REMOVE_OPERATION_CODE)
                ? null : seoMetadata.getFriendlyCodes();
        if (friendlyCodes != null) {
            List friendlyCodesList = new ArrayList<>();
            for (Entry<Object, Object> entry : friendlyCodes.entrySet()) {
                if (entry.getValue() instanceof PageMetatag) {
                    PageMetatag pageMetatag = (PageMetatag) entry.getValue();
                    friendlyCodesList.add(pageMetatag.getValue());
                }
            }
            this.getCacheWrapper().updateDraftPageReferences(friendlyCodesList, page.getCode());
        }
        if (!PageChangedEvent.EVENT_TYPE_SET_PAGE_OFFLINE.equals(event.getEventType())
                && !PageChangedEvent.EVENT_TYPE_SET_PAGE_ONLINE.equals(event.getEventType())
                && PageChangedEvent.REMOVE_OPERATION_CODE != event.getOperationCode()) {
            return;
        }
        if (PageChangedEvent.EVENT_TYPE_SET_PAGE_OFFLINE.equals(event.getEventType())) {
            this.getSeoMappingDAO().deleteMappingForPage(page.getCode());
        } else {
            this.getSeoMappingDAO().deleteMappingForPage(page.getCode());
            if (PageChangedEvent.REMOVE_OPERATION_CODE == event.getOperationCode()){
                // Delete draft page references if a page is deleted passing an empty ArrayList to
                // updateDraftPageReferences of the cacheWrapper
                this.getCacheWrapper().updateDraftPageReferences(new ArrayList<>(), page.getCode());
            } else if (PageChangedEvent.REMOVE_OPERATION_CODE != event.getOperationCode() && friendlyCodes != null) {

                for (Entry<Object, Object> entry : seoMetadata.getFriendlyCodes().entrySet()) {
                    if (entry.getValue() instanceof PageMetatag) {
                        PageMetatag pageMetatag = (PageMetatag) entry.getValue();
                        if (StringUtils.isEmpty(pageMetatag.getValue())) {
                            continue;
                        }
                        FriendlyCodeVO vo = new FriendlyCodeVO(pageMetatag.getValue(), page.getCode());
                        vo.setLangCode(pageMetatag.getLangCode());
                        this.getSeoMappingDAO().updateMapping(vo);
                    }
                }
            }
        }
        try {
            SeoChangedEvent seoEvent = new SeoChangedEvent();
            seoEvent.setOperationCode(SeoChangedEvent.PAGE_CHANGED_EVENT);
            this.notifyEvent(seoEvent);
            this.getCacheWrapper().initCache(this.getPageManager(), this.getSeoMappingDAO(), false);
        } catch (Throwable t) {
            logger.error("Error updating mapping from page changed", t);
        }
    }

    @Override
    public void updateFromPublicContentChanged(PublicContentChangedEvent event) {
        if (null == event.getContent()) {
            return;
        }
        Content content = event.getContent();
        try {
            if (event.getOperationCode() == PublicContentChangedEvent.REMOVE_OPERATION_CODE) {
                this.getSeoMappingDAO().deleteMappingForContent(content.getId());
            } else {
                Map<String, FriendlyCodeVO> existingMapping = this.getSeoMappingDAO().loadMapping();
                AttributeInterface attribute = content.getAttributeByRole(JpseoSystemConstants.ATTRIBUTE_ROLE_FRIENDLY_CODE);
                if (null == attribute || !(attribute instanceof ITextAttribute)) {
                    attribute = content.getAttributeByRole(JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE);
                }
                if (null != attribute && attribute instanceof ITextAttribute) {
                    ContentFriendlyCode contentFriendlyCode = this.prepareContentFriendlyCode(content.getId(), (ITextAttribute) attribute, existingMapping);
                    this.getSeoMappingDAO().updateMapping(contentFriendlyCode);
                }
            }
            SeoChangedEvent seoEvent = new SeoChangedEvent();
            seoEvent.setOperationCode(SeoChangedEvent.CONTENT_CHANGED_EVENT);
            this.notifyEvent(seoEvent);
            this.getCacheWrapper().initCache(this.getPageManager(), this.getSeoMappingDAO(), false);
        } catch (Throwable t) {
            logger.error("Error updating mapping from public content changed", t);
        }
    }

    private ContentFriendlyCode prepareContentFriendlyCode(String contentId, ITextAttribute attribute, Map<String, FriendlyCodeVO> existingMapping) throws EntException {
        ContentFriendlyCode contentFriendlyCode = new ContentFriendlyCode();
        contentFriendlyCode.setContentId(contentId);
        String defaultLang = this.getLangManager().getDefaultLang().getCode();
        if (((AttributeInterface) attribute).isMultilingual()) {
            String defaultFriendlyCode = this.generateUniqueFriendlyCode(attribute.getTextForLang(defaultLang), defaultLang);
            addFriendlyCode(contentFriendlyCode, existingMapping, defaultLang, defaultFriendlyCode);
            Iterator<Lang> langs = this.getLangManager().getLangs().iterator();
            while (langs.hasNext()) {
                Lang currentLang = langs.next();
                if (!currentLang.isDefault()) {
                    String langCode = currentLang.getCode();
                    String friendlyCode = this.generateUniqueFriendlyCode(attribute.getTextForLang(langCode), langCode);
                    if (friendlyCode != null && !friendlyCode.equals(defaultFriendlyCode)) {
                        addFriendlyCode(contentFriendlyCode, existingMapping, langCode, friendlyCode);
                    }
                }
            }
        } else {
            String friendlyCode = this.generateUniqueFriendlyCode(attribute.getText(), null);
            addFriendlyCode(contentFriendlyCode, existingMapping, defaultLang, friendlyCode);
        }
        List<String> langs = new ArrayList<>(contentFriendlyCode.getFriendlyCodes().keySet());
        for (int i = 0; i < langs.size(); i++) {
            String langCode = langs.get(i);
            String codesForLang = contentFriendlyCode.getFriendlyCodes().get(langCode);
            FieldSearchFilter<String> filterCode = new FieldSearchFilter<>("friendlycode", codesForLang, false);
            FieldSearchFilter<String> filterLang = new FieldSearchFilter<>("langcode", langCode, false);
            FieldSearchFilter[] filters = {filterCode, filterLang};
            List<String> codes = this.searchFriendlyCode(filters);
            if (null != codes && !codes.isEmpty()) {
                for (int j = 0; j < codes.size(); j++) {
                    FriendlyCodeVO codeVo = this.getCacheWrapper().getMappingByFriendlyCode(codes.get(j));
                    if (null != codeVo && (null == codeVo.getContentId() || !contentId.equals(codeVo.getContentId()))) {
                        logger.warn("Already existing mapping : code '{}' - contentId '{}' - pageCode '{}' - langCode '{}'",
                                codeVo.getFriendlyCode(), codeVo.getContentId(), codeVo.getPageCode(), codeVo.getLangCode());
                        contentFriendlyCode.getFriendlyCodes().remove(langCode);
                    }
                }
            }
        }
        return contentFriendlyCode;
    }

    private void addFriendlyCode(ContentFriendlyCode contentFriendlyCode, Map<String, FriendlyCodeVO> existingMapping, String lang, String friendlyCode) {
        String friendlyCodeToAdd = existingMapping.values().stream()
                .filter(vo -> contentFriendlyCode.getContentId().equals(vo.getContentId()) && lang.equals(vo.getLangCode()))
                .map(FriendlyCodeVO::getFriendlyCode)
                .findFirst().orElse(friendlyCode);
        contentFriendlyCode.addFriendlyCode(lang, friendlyCodeToAdd);
    }

    private String generateUniqueFriendlyCode(String originalText, String langCode) {
        String friendlyCode = FriendlyCodeGenerator.generateFriendlyCode(originalText);
        if (StringUtils.isBlank(originalText)) {
            return null;
        }
        FriendlyCodeVO existing = this.getReference(friendlyCode);
        if (null != existing) {
            int index = 1;
            String original = friendlyCode;
            do {
                friendlyCode = original + (null != langCode ? "_"+langCode : "") + "_" + index++;
            } while (null != this.getReference(friendlyCode));
        }
        return friendlyCode;
    }

    @Override
    public List<String> searchFriendlyCode(FieldSearchFilter[] filters) throws EntException {
        List<String> codes = null;
        try {
            codes = this.getSeoMappingDAO().searchFriendlyCode(filters);
        } catch (Throwable t) {
            logger.error("Error searching Friendly Codes", t);
            throw new EntException("Error searching Friendly Codes", t);
        }
        return codes;
    }

    @Override
    public String getDraftPageReference(String friendlyCode) {
        return this.getCacheWrapper().getDraftPageReference(friendlyCode);
    }

    @Override
    public FriendlyCodeVO getReference(String friendlyCode) {
        return this.getCacheWrapper().getMappingByFriendlyCode(friendlyCode);
    }

    @Override
    public String getContentReference(String contentId, String langCode) {
        String friendlyCode = null;
        ContentFriendlyCode content = this.getCacheWrapper().getMappingByContentId(contentId);
        if (content != null) {
            friendlyCode = content.getFriendlyCode(langCode);
            if (friendlyCode == null) {
                friendlyCode = content.getFriendlyCode(this.getLangManager().getDefaultLang().getCode());
            }
        }
        return friendlyCode;
    }

    public ISeoMappingDAO getSeoMappingDAO() {
        return seoMappingDAO;
    }
    public void setSeoMappingDAO(ISeoMappingDAO seoMappingDAO) {
        this.seoMappingDAO = seoMappingDAO;
    }

    protected ILangManager getLangManager() {
        return langManager;
    }
    public void setLangManager(ILangManager langManager) {
        this.langManager = langManager;
    }

    protected IPageManager getPageManager() {
        return pageManager;
    }
    public void setPageManager(IPageManager pageManager) {
        this.pageManager = pageManager;
    }

    protected ISeoMappingCacheWrapper getCacheWrapper() {
        return cacheWrapper;
    }
    public void setCacheWrapper(ISeoMappingCacheWrapper cacheWrapper) {
        this.cacheWrapper = cacheWrapper;
    }

}
