-- Copyright 2026 上海如静知华信息科技有限公司 · https:--www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2

-- 仅初始化结构；业务数据通过真实操作录入。
create table access_role (id bigint auto_increment primary key, name varchar(120) not null, scope varchar(20) not null, version bigint not null);

create table permission (id bigint auto_increment primary key, code varchar(60) not null unique, name varchar(120) not null);

create table nav_menu (id bigint auto_increment primary key, code varchar(60) not null unique, name varchar(120) not null, name_en varchar(120) not null, permission_code varchar(60) not null, position int not null, enabled boolean not null);

create table system_setting (id bigint auto_increment primary key, code varchar(60) not null unique, parameter_value varchar(6000) not null);

create table dictionary_entry (id bigint auto_increment primary key, type varchar(60) not null, code varchar(60) not null, name varchar(120) not null, name_en varchar(120) not null, enabled boolean not null, unique(type,code));

create table audit_event (id bigint auto_increment primary key, actor varchar(80) not null, action varchar(120) not null, object_id varchar(80) not null, department_id bigint not null, created_at timestamp(6) not null);

create table role_permission (role_id bigint not null,permission_code varchar(60) not null,primary key(role_id,permission_code),foreign key(role_id) references access_role(id),foreign key(permission_code) references permission(code));

create table department (
 id bigint auto_increment primary key,
 name varchar(120) not null,
 zone varchar(80) not null,
 enabled boolean not null,
 version bigint not null
);

create table account (id bigint auto_increment primary key, username varchar(60) not null unique, display_name varchar(120) not null, password_hash varchar(100) not null, role_id bigint not null, department_id bigint not null, enabled boolean not null, version bigint not null, foreign key(role_id) references access_role(id), foreign key(department_id) references department(id));

create table translation_project (
 id bigint auto_increment primary key,
 department_id bigint not null,
 owner_id bigint not null,
 translator_id bigint not null,
 reviewer_id bigint not null,
 name varchar(160) not null,
 source_locale varchar(40) not null,
 target_locale varchar(40) not null,
 platform varchar(60) not null,
 state varchar(20) not null,
 due_date date not null,
 created_at timestamp(6) not null,
 version bigint not null,
 foreign key(department_id) references department(id),
 foreign key(owner_id) references account(id),
 foreign key(translator_id) references account(id),
 foreign key(reviewer_id) references account(id)
);

create table segment (
 id bigint auto_increment primary key,
 project_id bigint not null,
 string_key varchar(160) not null,
 source varchar(4000) not null,
 target varchar(4000) not null,
 context varchar(1000) not null,
 max_length int not null,
 active boolean not null,
 status varchar(20) not null,
 edited_by bigint null,
 reviewed_by bigint null,
 review_note varchar(1000) not null,
 updated_at timestamp(6) not null,
 version bigint not null,
 foreign key(project_id) references translation_project(id),
 foreign key(edited_by) references account(id),
 foreign key(reviewed_by) references account(id),
 unique(project_id,string_key)
);

create table glossary_term (
 id bigint auto_increment primary key,
 project_id bigint not null,
 source varchar(200) not null,
 target varchar(200) not null,
 note varchar(500) not null,
 version bigint not null,
 foreign key(project_id) references translation_project(id),
 unique(project_id,source)
);

create table segment_revision (
 id bigint auto_increment primary key,
 segment_id bigint not null,
 project_id bigint not null,
 segment_version bigint not null,
 source varchar(4000) not null,
 target varchar(4000) not null,
 status varchar(20) not null,
 note varchar(1000) not null,
 action varchar(30) not null,
 actor_id bigint not null,
 created_at timestamp(6) not null,
 foreign key(segment_id) references segment(id),
 foreign key(project_id) references translation_project(id),
 foreign key(actor_id) references account(id)
);

create table project_event (
 id bigint auto_increment primary key,
 project_id bigint not null,
 action varchar(50) not null,
 actor_id bigint not null,
 note varchar(1000) not null,
 created_at timestamp(6) not null,
 foreign key(project_id) references translation_project(id),
 foreign key(actor_id) references account(id)
);

create table delivery (
 id bigint auto_increment primary key,
 project_id bigint not null,
 number int not null,
 name varchar(120) not null,
 project_version bigint not null,
 json_content longtext not null,
 csv_content longtext not null,
 sha256 varchar(64) not null,
 created_by bigint not null,
 created_at timestamp(6) not null,
 foreign key(project_id) references translation_project(id),
 foreign key(created_by) references account(id),
 unique(project_id,number)
);

create index idx_segment_project on segment(project_id,active,status);
create index idx_revision_segment on segment_revision(segment_id,segment_version);
create index idx_project_dept on translation_project(department_id,state);
create index idx_audit_dept on audit_event(department_id,created_at);
