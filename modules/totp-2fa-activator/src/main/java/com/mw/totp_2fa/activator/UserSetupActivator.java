package com.mw.totp_2fa.activator;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.mw.totp_2fa.key.model.SecretKey;
import com.mw.totp_2fa.key.service.SecretKeyLocalService;
import com.mw.totp_2fa.qrcode.service.QRCodeService;

/**
 * Bundle Activator that iterates through all users and for Active ones that don't have a Secret Key it creates a SecretKey and sends a QR Code URL to the user.
 * 
 *  This runs for ALL users even if the user is inactive / deleted.
 * 
 * @author Michael Wall
 *
 */
@Component(service = UserSetupActivator.class)
public class UserSetupActivator {

	@Activate
	protected void activate(Map<String, Object> properties) {
		_log.info("UsersCount: " + userLocalService.getUsersCount());	
		
		// Ideally we would use userLocalService.getUsers(companyId, defaultUser, status, start, end, obc) but we don't have companyId
		List<User> users = userLocalService.getUsers(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
		
		long secretKeysAdded = 0;

		for (User user : users) {
			if (!user.isDefaultUser() && user.isActive()) { //Only active non default users.
				SecretKey secretKeyObject = secretKeyLocalService.fetchSecretKeyByUserId(user.getCompanyId(), user.getUserId());
				
				if (secretKeyObject == null) {
					//Generate secret key
					secretKeyObject = secretKeyLocalService.addSecretKey(user);
					String secretKeyString = secretKeyObject.getSecretKey();
					
					qrCodeService.sendEmail(user, secretKeyString);
					
					secretKeysAdded++;
				}				
			}
		}
		
		if (_log.isInfoEnabled()) {
			_log.info("Added Secret Key to " + secretKeysAdded + " users.");
		}
	}

	@Reference(cardinality = ReferenceCardinality.MANDATORY, unbind = "-")
	private UserLocalService userLocalService;
	
	@Reference(cardinality = ReferenceCardinality.MANDATORY, unbind = "-")
	private SecretKeyLocalService secretKeyLocalService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, unbind = "-")
	private QRCodeService qrCodeService;
	
	private static Log _log = LogFactoryUtil.getLog(UserSetupActivator.class);
}