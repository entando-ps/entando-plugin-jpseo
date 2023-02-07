/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpseo.web.page.validator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.entando.entando.plugins.jpseo.aps.system.services.mapping.FriendlyCodeVO;
import org.entando.entando.plugins.jpseo.aps.system.services.mapping.ISeoMappingManager;
import org.entando.entando.plugins.jpseo.web.page.model.SeoDataByLang;
import org.entando.entando.web.page.validator.PageValidator;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
public class SeoPageValidator extends PageValidator {
    private static final EntLogger logger =  EntLogFactory.getSanitizedLogger(SeoPageValidator.class);
    public static final String ERRCODE_PAGE_INVALID_FRIENDLY_CODE = "10";
    public static final String ERRCODE_PAGE_DUPLICATED_FRIENDLY_CODE = "11";
    public static final String ERRCODE_PAGE_INVALID_TITLE = "12";

    @Autowired
    private ISeoMappingManager seoMappingManager;

    public boolean checkFriendlyCode(String pageCode, String friendlyCode) {
        if (null != friendlyCode && friendlyCode.trim().length() > 100) {
            logger.error("Invalid friendly Code {}", friendlyCode);
            return false;
        }
        if (null != friendlyCode && friendlyCode.trim().length() > 0) {
            Pattern pattern = Pattern.compile("([a-z0-9_])+");
            Matcher matcher = pattern.matcher(friendlyCode);
            if (!matcher.matches()) {
                logger.error("Invalid friendly Code {}", friendlyCode);
                return false;
            }
        }
        if (null != friendlyCode && friendlyCode.trim().length() > 0) {
            FriendlyCodeVO vo = this.seoMappingManager.getReference(friendlyCode);
            if (null != vo && (vo.getPageCode() == null || !vo.getPageCode().equals(pageCode))) {
                logger.error("Invalid friendly Code {}", friendlyCode);
                return false;
            }
            String draftPageReference = this.seoMappingManager.getDraftPageReference(friendlyCode);
            if (null != draftPageReference && !pageCode.equals(draftPageReference)) {
                logger.error("Invalid friendly Code {}", friendlyCode);
                return false;
            }
        }
        return true;
    }

    public boolean validateFriendlyCodeByLang(Map<String, SeoDataByLang> seoDataByLang, Errors errors) {

        List friendlyCodes = new ArrayList<String>();
        seoDataByLang.forEach(
                (key, value) -> {
                    if (null != value.getFriendlyCode() && !value.getFriendlyCode().isEmpty()) {
                        friendlyCodes.add(value.getFriendlyCode());
                    }
                });

        Set friendlyCodesDuplicates = findFriendlyCodesDuplicates(friendlyCodes);

        if (friendlyCodesDuplicates.size() > 0) {
            errors.reject(ERRCODE_PAGE_DUPLICATED_FRIENDLY_CODE, "Duplicated friendly code across different languages");
            return false;
        }
        return true;
    }

    private static <T> Set<T> findFriendlyCodesDuplicates(List<T> list) {
        return list.stream().filter(i -> Collections.frequency(list, i) > 1)
                .collect(Collectors.toSet());
    }

}
