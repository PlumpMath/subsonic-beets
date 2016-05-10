/* Email addresses are at maximum 254 characters long.
 * http://www.rfc-editor.org/errata_search.php?rfc=3696&eid=1690
 *
 * Though according to the PostgreSQL docs char, varchar and text all
 * share the same internal representation which means that we might as
 * well just use the text type for all text.
 */

create table users (
	id			 bigserial primary key,
	username text unique,
	email    text unique,
	salt		 text,
	password text
);
