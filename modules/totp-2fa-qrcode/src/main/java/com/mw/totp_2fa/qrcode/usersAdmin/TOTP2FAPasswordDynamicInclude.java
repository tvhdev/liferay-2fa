package com.mw.totp_2fa.qrcode.usersAdmin;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.servlet.taglib.BaseDynamicInclude;
import com.liferay.portal.kernel.servlet.taglib.DynamicInclude;
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.portlet.PortletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

/**
 * Renders the 2FA Secret Key / QR Code section into the Password screen of
 * both UsersAdminPortlet and MyAccountPortlet, inline with the
 * Password/WebDAV sections instead of appended below the whole form.
 *
 * <p>Hooked in via the totp-2fa-password-jsp fragment, which overrides
 * users-admin-web's password.jsp to add a single
 * {@code <liferay-util:dynamic-include key="com.liferay.users.admin.web#/user/password.jsp#totp2fa" />}
 * tag right after the WebDAV section. That tag fires this component during
 * the JSP's own rendering (for both a full portlet render and the AJAX
 * resource request the Edit User "Screen Navigation" tabs use), so no
 * RenderFilter/ResourceFilter response-rewriting is needed.</p>
 *
 * <p>Works directly off the raw {@code HttpServletRequest} the dynamic
 * include hands us (no {@code javax.portlet.PortletRequest} lookup): the
 * request already carries the current portlet id under
 * {@link WebKeys#PORTLET_ID}, and {@code ParamUtil}/{@code
 * PortletURLFactoryUtil} both have {@code HttpServletRequest}-based
 * overloads that already resolve parameters/URLs relative to that portlet's
 * namespace.</p>
 *
 * @author Michael Wall
 */
@Component(configurationPid = TOTP_2FAConfiguration.PID, service = DynamicInclude.class)
public class TOTP2FAPasswordDynamicInclude extends BaseDynamicInclude {

	private static final String KEY = "com.liferay.users.admin.web#/user/password.jsp#totp2fa";

	@Override
	public void register(DynamicInclude.DynamicIncludeRegistry dynamicIncludeRegistry) {
		dynamicIncludeRegistry.register(KEY);
	}

	@Override
	public void include(
			HttpServletRequest request, HttpServletResponse response, String key)
		throws IOException {

		try {
			String portletId = (String)request.getAttribute(WebKeys.PORTLET_ID);

			boolean isUserAdminScreen = UsersAdminPortletKeys.USERS_ADMIN.equals(
				portletId);
			boolean isMyAccountScreen = UsersAdminPortletKeys.MY_ACCOUNT.equals(
				portletId);

			if (!isUserAdminScreen && !isMyAccountScreen) {
				return;
			}

			long userId = ParamUtil.getLong(request, "p_u_i_d", -1);

			User user;

			if (isMyAccountScreen) {

				// My Account has no p_u_i_d parameter -- it's always the
				// current user.
				ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(
					WebKeys.THEME_DISPLAY);

				user = themeDisplay.getUser();
			}
			else if (userId > -1) {
				user = userLocalService.fetchUser(userId);
			}
			else {
				return;
			}

			if ((user == null) || !user.isActive()) {
				return;
			}

			// My Account's password.jsp only shows the 2FA section once 2FA is
			// actually enabled portal-wide; Users Admin always shows it (an
			// administrator managing users' secret keys regardless of the
			// current login-enforcement setting), matching the previous
			// portlet filters' behavior.
			if (isMyAccountScreen && !tfaConfiguration.loginTotp2faEnabled()) {
				return;
			}

			SecretKey secretKeyObject = secretKeyLocalService.fetchSecretKeyByUserId(
				user.getCompanyId(), user.getUserId());

			boolean hasSecretKey =
				(secretKeyObject != null) &&
				!Validator.isNull(secretKeyObject.getSecretKey());

			PrintWriter printWriter = response.getWriter();

			printWriter.write(
				getContent(
					isUserAdminScreen, tfaConfiguration.showSecretKeysOnAccountScreens(),
					hasSecretKey, portletId, request, user, secretKeyObject));
		}
		catch (Exception exception) {
			_log.error(
				"Unable to render the 2FA section on the Password screen",
				exception);
		}
	}

	private String getContent(
		boolean isUserAdminScreen, boolean showSecretKeysOnAccountScreens,
		boolean hasSecretKey, String portletId, HttpServletRequest request, User user,
		SecretKey secretKeyObject) {

		StringBuilder customText = new StringBuilder();

		String generateSecretKeyLabel;
		String sendEmailLabel = null;

		// Custom label based on screen type and whether the user already has a
		// secret key...
		if (isUserAdminScreen) {
			generateSecretKeyLabel = LanguageUtil.get(
				request.getLocale(), "generate-secret-key-and-email-qr-code");

			if (hasSecretKey) {
				generateSecretKeyLabel = LanguageUtil.get(
					request.getLocale(), "regenerate-secret-key-and-email-qr-code");
				sendEmailLabel = LanguageUtil.get(
					request.getLocale(), "email-2fa-qr-code-url");
			}
		}
		else {
			generateSecretKeyLabel = LanguageUtil.get(
				request.getLocale(), "generate-secret-key");

			if (hasSecretKey) {
				generateSecretKeyLabel = LanguageUtil.get(
					request.getLocale(), "regenerate-secret-key");
			}
		}

		String tfaRequiredOnLoginLabel = LanguageUtil.get(
			request.getLocale(), "2fa-required-on-login-when-2fa-enabled");

		String anyUnsavedChangesLabel = LanguageUtil.get(
			request.getLocale(), "any-unsaved-changed-to-this-screen-will-be-lost");

		boolean[] tfaSkip = TOTP_2FAUtil.isAdministratorOrSkipUserRole(user);

		ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(
			WebKeys.THEME_DISPLAY);

		// The Action URL to call the custom MVCActionCommand
		LiferayPortletURL generateSecretKeyActionURL = PortletURLFactoryUtil.create(
			request, portletId, PortletRequest.ACTION_PHASE);
		generateSecretKeyActionURL.setParameter(
			"p_u_i_d", String.valueOf(user.getUserId()));

		if (hasSecretKey) { // Regenerating
			generateSecretKeyActionURL.setParameter(
				"javax.portlet.action", "/users_admin/regenerate_2fa_secret_key");
		}
		else { // Generating for first time
			generateSecretKeyActionURL.setParameter(
				"javax.portlet.action", "/users_admin/generate_2fa_secret_key");
		}

		generateSecretKeyActionURL.setParameter(
			"redirect", themeDisplay.getURLCurrent());
		generateSecretKeyActionURL.setParameter(
			"sendEmail", Boolean.toString(isUserAdminScreen)); // Send an email
																// only if not
																// myAccount.

		// Only applicable when User Admin and Has Secret Key
		if (hasSecretKey && isUserAdminScreen) {
			LiferayPortletURL sendEmailActionURL = PortletURLFactoryUtil.create(
				request, portletId, PortletRequest.ACTION_PHASE);
			sendEmailActionURL.setParameter(
				"p_u_i_d", String.valueOf(user.getUserId()));
			sendEmailActionURL.setParameter(
				"javax.portlet.action", "/users_admin/email_2fa_qr_code_url");

			sendEmailActionURL.setParameter(
				"redirect", themeDisplay.getURLCurrent());

			customText.append(
				"<a href=\"" + sendEmailActionURL.toString() + "\">" +
					sendEmailLabel + "</a>&nbsp;" + anyUnsavedChangesLabel);
			customText.append("<br>");
		}

		customText.append(
			"<a href=\"" + generateSecretKeyActionURL.toString() + "\">" +
				generateSecretKeyLabel + "</a>&nbsp;" + anyUnsavedChangesLabel);
		customText.append("<br>");
		customText.append("<strong>" + tfaRequiredOnLoginLabel + "</strong>");
		customText.append("<br>");

		// Whether the user (currently) needs a code to login (where 2FA
		// enabled)
		if (!tfaSkip[0]) {
			customText.append(LanguageUtil.get(request.getLocale(), "yes"));
		}
		else {
			customText.append(LanguageUtil.get(request.getLocale(), "no"));
		}

		if (hasSecretKey) { // Show the secret key and the QR Code image
			if (showSecretKeysOnAccountScreens) {
				String secretKeyString = secretKeyObject.getSecretKey();
				String secretKeyLabel = LanguageUtil.get(
					request.getLocale(), "2fa-secret-key");

				customText.append("<br><br>");
				customText.append("<strong>" + secretKeyLabel + "</strong>");
				customText.append("<br>");
				customText.append(secretKeyString);
			}

			String qrCodeUrl = qrCodeService.getQRCodeURL(
				user, QRCodeConstants.QR_CODE_JWT_URL_TYPE.WEB);
			String qrCodeLabel = LanguageUtil.get(
				request.getLocale(), "2fa-qr-code");

			customText.append("<br><br>");
			customText.append("<strong>" + qrCodeLabel + "</strong>");
			customText.append("<br>");
			customText.append(
				"<img src=\"" + qrCodeUrl + "\" alt=\"" + qrCodeLabel +
					"\" height=\"" + QRCodeConstants.QR_CODE_HEIGHT + "\" width=\"" +
					QRCodeConstants.QR_CODE_WIDTH + "\">");
		}

		return customText.toString();
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		tfaConfiguration = ConfigurableUtil.createConfigurable(
			TOTP_2FAConfiguration.class, properties);
	}

	private volatile TOTP_2FAConfiguration tfaConfiguration;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, unbind = "-")
	private UserLocalService userLocalService;

	// Excludes the raw, unproxied AopService-tagged bean: Liferay's AOP
	// extender registers a SEPARATE, transactionally-wrapped proxy service
	// (without AopService in its objectClass) alongside the raw one, and
	// consumers that race-bind to the raw one at startup get
	// "IllegalStateException: No current transaction executor" on writes.
	@Reference(
		cardinality = ReferenceCardinality.MANDATORY,
		target = "(!(objectClass=com.liferay.portal.aop.AopService))",
		unbind = "-"
	)
	private SecretKeyLocalService secretKeyLocalService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, unbind = "-")
	private QRCodeService qrCodeService;

	private static final Log _log = LogFactoryUtil.getLog(
		TOTP2FAPasswordDynamicInclude.class);

}
