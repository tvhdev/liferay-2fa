package com.mw.totp_2fa.qrcode.web.internal.frontend.taglib.servlet.taglib;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletRequest;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.liferay.frontend.taglib.servlet.taglib.ScreenNavigationEntry;
import com.liferay.frontend.taglib.servlet.taglib.util.JSPRenderer;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.users.admin.constants.UsersAdminPortletKeys;
import com.mw.totp_2fa.config.TOTP_2FAConfiguration;
import com.mw.totp_2fa.key.model.SecretKey;
import com.mw.totp_2fa.key.service.SecretKeyLocalService;
import com.mw.totp_2fa.qrcode.constants.QRCodeConstants;
import com.mw.totp_2fa.qrcode.service.QRCodeService;
import com.mw.totp_2fa.util.TOTP_2FAUtil;

@Component(
		immediate = true,
	    configurationPid = TOTP_2FAConfiguration.PID,
		property = {
			"screen.navigation.entry.order:Integer=60"
		},
		service = {TOTPScreenNavigationEntry.class}
	)
public class TOTPScreenNavigationEntry
implements ScreenNavigationEntry<User> {

	@Override
	public boolean isVisible(User user, User context) {
		_log.info("TOTP isVisible called...");
		return tfaConfiguration.loginTotp2faEnabled() == true;
	}

	@Override
	public String getCategoryKey() {
		return "general";
	}

	@Override
	public String getEntryKey() {
		return "general";
	}

	@Override
	public String getLabel(Locale locale) {
		return "2-Faktor Authentifizierung (TOTP)";
	}

	@Override
	public String getScreenNavigationKey() {
		return "edit.user.form";
	}
	
	public String getContent(boolean isUserAdminScreen, boolean showSecretKeysOnAccountScreens, QRCodeService qrCodeService, boolean hasSecretKey, String portletId, HttpServletRequest request, User user, SecretKey secretKeyObject) {
		StringBuilder customText = new StringBuilder();

		String generateSecretKeyLabel = null;
		String sendEmailLabel = null;

		//Custom label based on screen type and whether the user already has a secret key...
		if (isUserAdminScreen) {
			generateSecretKeyLabel = LanguageUtil.get(request.getLocale(), "generate-secret-key-and-email-qr-code");
			
			if (hasSecretKey) {
				generateSecretKeyLabel = LanguageUtil.get(request.getLocale(), "regenerate-secret-key-and-email-qr-code");
				sendEmailLabel = LanguageUtil.get(request.getLocale(), "email-2fa-qr-code-url");
			}
		} else {
			generateSecretKeyLabel = LanguageUtil.get(request.getLocale(), "generate-secret-key");
			
			if (hasSecretKey) {
				generateSecretKeyLabel = LanguageUtil.get(request.getLocale(), "regenerate-secret-key");
			}
		}

		String tfaRequiredOnLoginLabel = LanguageUtil.get(request.getLocale(),
				"2fa-required-on-login-when-2fa-enabled");
		
		String anyUnsavedChangesLabel = LanguageUtil.get(request.getLocale(),
				"any-unsaved-changed-to-this-screen-will-be-lost");

		boolean[] tfaSkip = TOTP_2FAUtil.isAdministratorOrSkipUserRole(user);

		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);

		//The Action URL to call the custom MVCActionCommand
		LiferayPortletURL generateSecretKeyActionURL = PortletURLFactoryUtil.create(request,
				portletId, PortletRequest.ACTION_PHASE);
		generateSecretKeyActionURL.setParameter("p_u_i_d", String.valueOf(user.getUserId()));
		if (hasSecretKey) {//Regenerating
			generateSecretKeyActionURL.setParameter("javax.portlet.action", "/users_admin/regenerate_2fa_secret_key");
		} else {//Generating for first time
			generateSecretKeyActionURL.setParameter("javax.portlet.action", "/users_admin/generate_2fa_secret_key");
		}
		
		generateSecretKeyActionURL.setParameter("redirect", themeDisplay.getURLCurrent());
		generateSecretKeyActionURL.setParameter("sendEmail", Boolean.toString(isUserAdminScreen)); //Send an email only if not myAccount.

		//Only applicable when User Admin and Has Secret Key
		if (hasSecretKey && isUserAdminScreen) {
			LiferayPortletURL sendEmailActionURL = PortletURLFactoryUtil.create(request,
					portletId, PortletRequest.ACTION_PHASE);
			sendEmailActionURL.setParameter("p_u_i_d", String.valueOf(user.getUserId()));
			sendEmailActionURL.setParameter("javax.portlet.action", "/users_admin/email_2fa_qr_code_url");
			
			sendEmailActionURL.setParameter("redirect", themeDisplay.getURLCurrent());
			
			// TODO MW Change to buttons...
			customText.append("<a href=\"" + sendEmailActionURL.toString() + "\">" + sendEmailLabel + "</a>&nbsp;" + anyUnsavedChangesLabel);
			customText.append("<br>");
		}

		// TODO MW Change to buttons...
		customText.append("<a href=\"" + generateSecretKeyActionURL.toString() + "\">" + generateSecretKeyLabel + "</a>&nbsp;" + anyUnsavedChangesLabel);
		customText.append("<br>");
		customText.append("<strong>" + tfaRequiredOnLoginLabel + "</strong>");
		customText.append("<br>");
		
		//Whether the user (currently) needs a code to login (where 2FA enabled)
		if (!tfaSkip[0]) {
			String yesLabel = LanguageUtil.get(request.getLocale(), "yes");
			customText.append(yesLabel);
		} else {
			String noLabel = LanguageUtil.get(request.getLocale(), "no");
			customText.append(noLabel);
		}

		if (hasSecretKey) { // Show the secret key and the QR Code image
			if (showSecretKeysOnAccountScreens) {
				String secretKeyString = secretKeyObject.getSecretKey();
				String secretKeyLabel = LanguageUtil.get(request.getLocale(), "2fa-secret-key");
				
				customText.append("<br><br>");
				customText.append("<strong>" + secretKeyLabel + "</strong>");
				customText.append("<br>");
				customText.append(secretKeyString);				
			}
			
			String qrCodeUrl = qrCodeService.getQRCodeURL(user, QRCodeConstants.QR_CODE_JWT_URL_TYPE.WEB);
			String qrCodeLabel = LanguageUtil.get(request.getLocale(), "2fa-qr-code");

			customText.append("<br><br>");
			customText.append("<strong>" + qrCodeLabel + "</strong>");
			customText.append("<br>");
			customText.append("<img src=\"" + qrCodeUrl + "\" alt=\"" + qrCodeLabel + "\" height=\""
					+ QRCodeConstants.QR_CODE_HEIGHT + "\" width=\"" + QRCodeConstants.QR_CODE_WIDTH + "\">");
		}
		
		return customText.toString();
	}

	@Override
	public void render(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws IOException {

		long userId = ParamUtil.getLong(httpServletRequest, "p_u_i_d", -1);
		
		User user = userLocalService.fetchUser(userId);
		
		SecretKey secretKeyObject = secretKeyLocalService.fetchSecretKeyByUserId(user.getCompanyId(), user.getUserId());
		
		boolean hasSecretKey = false;
		
		if (secretKeyObject != null && !Validator.isNull(secretKeyObject.getSecretKey())) {
			hasSecretKey = true;
		}
		
		String html = getContent(true, tfaConfiguration.showSecretKeysOnAccountScreens(), qrCodeService, hasSecretKey, UsersAdminPortletKeys.USERS_ADMIN, httpServletRequest, user, secretKeyObject);
		httpServletResponse.getWriter().write(html);
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_log.info("TOTP Screen Navigation Entry activated / modified...");
		tfaConfiguration = ConfigurableUtil.createConfigurable(TOTP_2FAConfiguration.class, properties);
	}
	
	private static Log _log = LogFactoryUtil.getLog(TOTPScreenNavigationEntry.class);	

	private volatile TOTP_2FAConfiguration tfaConfiguration;	
	
	@Reference
	private SecretKeyLocalService secretKeyLocalService;
	
	@Reference
	private UserLocalService userLocalService;
	
	@Reference
	private QRCodeService qrCodeService;	
	
	@Reference
	private JSPRenderer _jspRenderer;

	@Reference(target = "(osgi.web.symbolicname=com.mw.totp-2fa.qrcode)")
	private ServletContext _servletContext;
}