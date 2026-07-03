create unique index IX_29DC4FD5 on totp_SecretKey (companyId, userId);
create index IX_82AB19A7 on totp_SecretKey (uuid_[$COLUMN_LENGTH:75$]);