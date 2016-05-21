create table authorization_tokens (
	users_id bigserial references users(id) on delete cascade,
	token    text      not null
);
