package com.mw.totp_2fa.login.auth;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.servlet.taglib.BaseDynamicInclude;
import com.liferay.portal.kernel.servlet.taglib.DynamicInclude;
import com.mw.totp_2fa.config.TOTP_2FAConfiguration;
import com.mw.totp_2fa.login.auth.constants.LoginConstants;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

/**
 * Renders the Authenticator Code field into the Login portlet form, right
 * after the password caps-lock span.
 *
 * <p>Hooked in via the totp-2fa-login-jsp fragment, which overrides
 * login.web's login.jsp to add a single
 * {@code <liferay-util:dynamic-include key="com.liferay.login.web#/login.jsp#totp2fa" />}
 * tag inside the form's fieldset. This replaces the previous
 * LoginPortletFilter, which spliced the same markup into the rendered
 * response HTML by string search/replace.</p>
 *
 * @author Michael Wall
 */
@Component(configurationPid = TOTP_2FAConfiguration.PID, service = DynamicInclude.class)
public class TOTP2FALoginDynamicInclude extends BaseDynamicInclude {

	private static final String KEY = "com.liferay.login.web#/login.jsp#totp2fa";

	@Override
	public void register(DynamicInclude.DynamicIncludeRegistry dynamicIncludeRegistry) {
		dynamicIncludeRegistry.register(KEY);
	}

	@Override
	public void include(
			HttpServletRequest request, HttpServletResponse response, String key)
		throws IOException {

		if (!tfaConfiguration.loginTotp2faEnabled()) {
			return;
		}

		PrintWriter printWriter = response.getWriter();

		printWriter.write(getAuthenticatorCodeHTML(request.getLocale()));
	}

	// Renders the Authenticator Code as a row of single-digit boxes (PayPal /
	// bank-app style) instead of one free-text input. The individual boxes'
	// values are combined into a hidden input using the original
	// AUTHENTICATOR_CODE_FIELD name, so TOTP_2FAPostAuthenticator's parameter
	// reading needs no changes.
	//
	// Wiring is done with inline on* attributes (oninput/onkeydown/onpaste),
	// not a <script> tag: the Login form here can also be rendered inside
	// Liferay's modal "Sign In" dialog, which fetches this markup over AJAX
	// and injects it via an innerHTML-based mechanism (confirmed empirically
	// -- a <script> tag placed in this same spot never executes there, even
	// though the identical markup works on a normal full-page render). Per
	// the HTML spec, script elements inserted that way are never executed,
	// with no workaround possible from inside the script itself. Inline on*
	// attributes don't have that restriction: they're wired up by the parser
	// as part of creating the element, regardless of insertion method. Liferay's
	// own login.jsp already relies on exactly this for the same modal (the
	// form's onSubmit="event.preventDefault();" and the "Remember Me"
	// checkbox's onclick), which is what confirms it's safe to use here too.
	//
	// Each handler is self-contained and resolves everything relative to
	// `this`/`event.target` (closest('.form-group'), not getElementById), so
	// no ids need to be globally unique even if the Login form renders more
	// than once on the same page (e.g. a persistent fast-login widget behind
	// the modal).
	private String getAuthenticatorCodeHTML(Locale locale) {

		int authenticatorCodeLength = tfaConfiguration.authenticatorCodeLength();

		String fieldId = LoginConstants.AUTHENTICATOR_CODE_FIELD;
		String domIdPrefix = "totp2faOtp" + _instanceCounter.incrementAndGet();
		String label = LanguageUtil.get(locale, "authenticator-code");

		StringBuilder html = new StringBuilder();

		html.append("<div class=\"form-group input-text-wrapper\" style=\"text-align:center;\">");
		html.append("<label class=\"control-label\" for=\"" + domIdPrefix + "0\">" + label + "</label>");

		// Grid, not flex, so the group is capped at exactly the width of
		// authenticatorCodeLength boxes (never wider) while every column
		// still shrinks evenly if the surrounding form is narrower than that
		// -- e.g. the modal "Sign In" dialog, which renders noticeably
		// narrower than the full login page.
		String boxWidth = "2.5em";
		String gap = "8px";

		html.append("<style>");
		html.append(".totp-2fa-otp-group{display:grid;gap:" + gap + ";margin:0 auto;"
				+ "grid-template-columns:repeat(" + authenticatorCodeLength + ",minmax(0,1fr));"
				+ "max-width:calc(" + authenticatorCodeLength + " * " + boxWidth + " + "
				+ (authenticatorCodeLength - 1) + " * " + gap + ");}");
		html.append(".totp-2fa-otp-input{width:100%;height:2.75em;padding:0;text-align:center;font-size:1.25em;box-sizing:border-box;}");
		html.append("</style>");

		html.append("<div class=\"totp-2fa-otp-group\" role=\"group\" aria-label=\"" + label + "\">");

		for (int i = 0; i < authenticatorCodeLength; i++) {
			html.append("<input class=\"totp-2fa-otp-input field form-control\" id=\"" + domIdPrefix + i
					+ "\" type=\"text\" inputmode=\"numeric\" pattern=\"[0-9]*\" maxlength=\"1\" autocomplete=\""
					+ (i == 0 ? "one-time-code" : "off") + "\""
					+ " oninput=\"" + INPUT_HANDLER + "\""
					+ " onkeydown=\"" + KEYDOWN_HANDLER + "\""
					+ " onpaste=\"" + PASTE_HANDLER + "\">");
		}

		html.append("</div>");
		html.append("<input type=\"hidden\" class=\"totp-2fa-otp-hidden\" name=\"" + fieldId + "\" value=\"\" aria-required=\"true\">");

		html.append("</div>");

		return html.toString();
	}

	private static final AtomicLong _instanceCounter = new AtomicLong();

	// var w = the shared .form-group wrapper, d = this box's sibling boxes
	// (in DOM order), i = this box's index within d, h = the hidden input
	// that gets submitted.
	private static final String SCOPE_PREAMBLE =
		"var w=this.closest('.form-group'),"
		+ "d=Array.prototype.slice.call(w.querySelectorAll('.totp-2fa-otp-input')),"
		+ "i=d.indexOf(this),"
		+ "h=w.querySelector('.totp-2fa-otp-hidden');";

	private static final String AUTO_SUBMIT =
		"if(d.every(function(x){return x.value.length===1;})){"
		+ "var f=this.closest('form');"
		+ "if(f){if(f.requestSubmit){f.requestSubmit();}"
		+ "else{f.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));}}}";

	private static final String INPUT_HANDLER =
		"this.value=this.value.replace(/[^0-9]/g,'').slice(-1);"
		+ SCOPE_PREAMBLE
		+ "if(this.value&&i<d.length-1){d[i+1].focus();}"
		+ "h.value=d.map(function(x){return x.value;}).join('');"
		+ "if(i===d.length-1&&this.value){" + AUTO_SUBMIT + "}";

	private static final String KEYDOWN_HANDLER =
		SCOPE_PREAMBLE
		+ "if(event.key==='Backspace'&&!this.value&&i>0){d[i-1].focus();}"
		+ "else if(event.key==='ArrowLeft'&&i>0){d[i-1].focus();}"
		+ "else if(event.key==='ArrowRight'&&i<d.length-1){d[i+1].focus();}";

	private static final String PASTE_HANDLER =
		"event.preventDefault();"
		+ SCOPE_PREAMBLE
		+ "var p=(event.clipboardData||window.clipboardData).getData('text').replace(/[^0-9]/g,'');"
		+ "for(var k=0;k<p.length&&(i+k)<d.length;k++){d[i+k].value=p.charAt(k);}"
		+ "var n=Math.min(i+p.length,d.length-1);d[n].focus();"
		+ "h.value=d.map(function(x){return x.value;}).join('');"
		+ AUTO_SUBMIT;

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		tfaConfiguration = ConfigurableUtil.createConfigurable(TOTP_2FAConfiguration.class, properties);
	}

	private volatile TOTP_2FAConfiguration tfaConfiguration;

}
