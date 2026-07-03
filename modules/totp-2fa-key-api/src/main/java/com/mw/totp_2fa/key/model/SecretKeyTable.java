/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.mw.totp_2fa.key.model;

import com.liferay.petra.sql.dsl.Column;
import com.liferay.petra.sql.dsl.base.BaseTable;

import java.sql.Types;

/**
 * The table class for the &quot;totp_SecretKey&quot; database table.
 *
 * @author Brian Wing Shun Chan
 * @see SecretKey
 * @generated
 */
public class SecretKeyTable extends BaseTable<SecretKeyTable> {

	public static final SecretKeyTable INSTANCE = new SecretKeyTable();

	public final Column<SecretKeyTable, String> uuid = createColumn(
		"uuid_", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<SecretKeyTable, Long> secretKeyId = createColumn(
		"secretKeyId", Long.class, Types.BIGINT, Column.FLAG_PRIMARY);
	public final Column<SecretKeyTable, Long> companyId = createColumn(
		"companyId", Long.class, Types.BIGINT, Column.FLAG_DEFAULT);
	public final Column<SecretKeyTable, Long> userId = createColumn(
		"userId", Long.class, Types.BIGINT, Column.FLAG_DEFAULT);
	public final Column<SecretKeyTable, String> secretKey = createColumn(
		"secretKey", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);

	private SecretKeyTable() {
		super("totp_SecretKey", SecretKeyTable::new);
	}

}