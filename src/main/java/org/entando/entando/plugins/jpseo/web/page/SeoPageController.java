package org.entando.entando.plugins.jpseo.web.page;

import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.user.UserDetails;

import java.util.*;
import java.util.Map.Entry;
import org.entando.entando.aps.system.services.page.IPageService;
import org.entando.entando.aps.system.services.page.PageAuthorizationService;
import org.entando.entando.aps.system.services.page.model.PageDto;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.plugins.jpseo.aps.system.services.page.SeoPageDto;
import org.entando.entando.plugins.jpseo.aps.system.services.page.SeoPageService;
import org.entando.entando.plugins.jpseo.web.page.model.SeoDataByLang;
import org.entando.entando.plugins.jpseo.web.page.model.SeoPageRequest;
import org.entando.entando.plugins.jpseo.web.page.validator.SeoPageValidator;
import org.entando.entando.web.common.exceptions.ResourcePermissionsException;
import org.entando.entando.web.common.exceptions.ValidationConflictException;
import org.entando.entando.web.common.exceptions.ValidationGenericException;
import org.entando.entando.web.common.model.RestResponse;
import org.entando.entando.web.common.model.SimpleRestResponse;
import org.entando.entando.web.page.model.PagePositionRequest;
import org.entando.entando.web.page.model.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttributes;

@RestController
@RequestMapping(value = "/plugins/seo/pages")
@SessionAttributes("user")
public class SeoPageController implements ISeoPageController {

    private final EntLogger logger =  EntLogFactory.getSanitizedLogger(getClass());

    @Autowired
    @Qualifier("SeoPageService")
    private IPageService pageService;

    @Autowired
    private SeoPageValidator seoPageValidator;

    @Autowired
    private PageAuthorizationService authorizationService;

    public IPageService getPageService() {
        return pageService;
    }

    public void setPageService(SeoPageService pageService) {
        this.pageService = pageService;
    }

    public SeoPageValidator getSeoPageValidator() {
        return seoPageValidator;
    }

    public void setSeoPageValidator(SeoPageValidator seoPageValidator) {
        this.seoPageValidator = seoPageValidator;
    }

    public PageAuthorizationService getAuthorizationService() {
        return authorizationService;
    }

    public void setAuthorizationService(PageAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public ResponseEntity<RestResponse<PageDto, Map<String, String>>> getSeoPage(UserDetails user, String pageCode,
            String status) {
        logger.debug("get seo page {}", pageCode);
        Map<String, String> metadata = new HashMap<>();
        if (!this.getAuthorizationService().isAuth(user, pageCode)) {
            return new ResponseEntity<>(new RestResponse<>(new PageDto(), metadata), HttpStatus.UNAUTHORIZED);
        }
        SeoPageDto page = (SeoPageDto) this.getPageService().getPage(pageCode, status);
        metadata.put("status", status);
        return new ResponseEntity<>(new RestResponse<>(page, metadata), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SimpleRestResponse<SeoPageDto>> addPage(UserDetails user,
            SeoPageRequest pageRequest, BindingResult bindingResult) throws EntException {
        logger.debug("creating page with request {}", pageRequest);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        getSeoPageValidator().validate(pageRequest, bindingResult);
        if ((null!=pageRequest.getSeoData()) && (null!=pageRequest.getSeoData().getSeoDataByLang())) {
            for (Entry<String, SeoDataByLang> entry : pageRequest.getSeoData().getSeoDataByLang().entrySet()) {
                if (!getSeoPageValidator().checkFriendlyCode(pageRequest.getCode(), entry.getValue().getFriendlyCode())) {
                    DataBinder binder = new DataBinder(entry.getValue());
                    bindingResult = binder.getBindingResult();
                    bindingResult.reject("10",  "Invalid friendly code");
                    throw new ValidationConflictException(bindingResult);
                }
            }
        }

        if (null!=pageRequest.getSeoData()) {
            getSeoPageValidator().validateFriendlyCodeByLang(pageRequest.getSeoData().getSeoDataByLang(), bindingResult);
        }
        if (bindingResult.hasErrors()) {
            throw new ValidationConflictException(bindingResult);
        }
        validatePagePlacement(pageRequest, bindingResult);

        SeoPageDto dto = (SeoPageDto) this.getPageService().addPage(pageRequest);
        return new ResponseEntity<>(new SimpleRestResponse<>(dto), HttpStatus.OK);
    }

    private void validatePagePlacement(PageRequest pageRequest, BindingResult bindingResult) {
        IPage parent = seoPageValidator.getDraftPage(pageRequest.getParentCode());
        seoPageValidator.validateGroups(pageRequest.getOwnerGroup(), parent.getGroup(), bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
    }

    @Override
    public ResponseEntity<RestResponse<SeoPageDto, Map<String, String>>> updatePage(
            UserDetails user,String pageCode,
           SeoPageRequest pageRequest, BindingResult bindingResult) {
        logger.debug("updating page {} with request {}", pageCode, pageRequest);

        if (!this.getAuthorizationService().isAuth(user, pageCode)) {
            throw new ResourcePermissionsException(bindingResult, user.getUsername(), pageCode);
        }
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        this.getSeoPageValidator().validateBodyCode(pageCode, pageRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }

        if (null!=pageRequest.getSeoData()) {
            getSeoPageValidator().validateFriendlyCodeByLang(pageRequest.getSeoData().getSeoDataByLang(), bindingResult);
        }
        if (bindingResult.hasErrors()) {
            throw new ValidationConflictException(bindingResult);
        }

        if ((null != pageRequest.getSeoData()) && (null != pageRequest.getSeoData().getSeoDataByLang())) {
            for (Entry<String, SeoDataByLang> entry : pageRequest.getSeoData().getSeoDataByLang().entrySet()) {
                if (!getSeoPageValidator().checkFriendlyCode(pageRequest.getCode(), entry.getValue().getFriendlyCode())) {
                    DataBinder binder = new DataBinder(entry.getValue());
                    bindingResult = binder.getBindingResult();
                    bindingResult.reject("10",  "Invalid friendly code");
                    throw new ValidationConflictException(bindingResult);
                }
            }
        }
        PagePositionRequest pagePositionRequest = new PagePositionRequest();
        pagePositionRequest.setParentCode(pageRequest.getParentCode());
        pagePositionRequest.setCode(pageCode);
        int position = pageService.getPages(pageCode).size() + 1;
        pagePositionRequest.setPosition(position);
        this.getSeoPageValidator().validateMovePage(pageCode, bindingResult, pagePositionRequest);
        SeoPageDto page = (SeoPageDto) pageService.updatePage(pageCode, pageRequest);
        Map<String, String> metadata = new HashMap<>();
        return new ResponseEntity<>(new RestResponse<>(page, metadata), HttpStatus.OK);
    }

}
