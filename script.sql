create table chat_message
(
    id          bigint auto_increment
        primary key,
    chat_id     varchar(64) charset utf8            not null,
    role        varchar(16) charset utf8            not null,
    content     text                                not null,
    create_time timestamp default CURRENT_TIMESTAMP null
)
    engine = InnoDB;

create definer = root@localhost trigger AfterChatMessageInsert_UpdateSessionActivity
    after insert
    on chat_message
    for each row
begin
    -- missing source code
end;

create table user
(
    id          bigint unsigned auto_increment comment '主键'
        primary key,
    openid      varchar(64)                        not null comment '微信 openid',
    username    varchar(64)                        not null comment '用户名',
    nickname    varchar(64)                        null comment '昵称',
    avatar      text                               null comment '头像 URL',
    mobile      varchar(20)                        null comment '手机号',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint openid
        unique (openid)
)
    comment '用户表' engine = InnoDB;

create table family
(
    id       bigint unsigned auto_increment comment '主键'
        primary key,
    userid   bigint unsigned not null comment '外键',
    name     varchar(255)    not null comment '姓名',
    gender   varchar(10)     not null comment '性别',
    age      int             not null comment '年龄',
    relation varchar(255)    not null comment '关系',
    constraint family_user_id_fk
        foreign key (userid) references user (id)
)
    comment '家人' engine = InnoDB
                   charset = utf8;

create table report
(
    id           bigint auto_increment
        primary key,
    username     varchar(100)    null,
    gender       varchar(10)     null,
    age          int             null,
    symptoms     text            null,
    duration     varchar(100)    null,
    treatment    text            null,
    other        text            null,
    check_time   datetime        null,
    image_url    text            null,
    disease_type varchar(100)    null,
    value        varchar(255)    null,
    advice       text            null,
    introduction text            null,
    userid       bigint unsigned not null comment '用户外键',
    constraint report_user_id_fk
        foreign key (userid) references user (id)
)
    engine = InnoDB
    charset = utf8;

create table concept_dictionary
(
    id            bigint auto_increment
        primary key,
    concept_index int          not null,
    name_en       varchar(128) null,
    name_cn       varchar(128) null,
    create_time   datetime default CURRENT_TIMESTAMP null,
    update_time   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uq_concept_dictionary_index
        unique (concept_index)
)
    engine = InnoDB
    charset = utf8;

create table report_concept_score
(
    id              bigint auto_increment
        primary key,
    report_id       bigint       not null,
    concept_index   int          not null,
    concept_name_en varchar(128) null,
    concept_name_cn varchar(128) null,
    score           double       not null,
    rank_no         int          not null,
    create_time     datetime default CURRENT_TIMESTAMP null,
    constraint fk_report_concept_score_report
        foreign key (report_id) references report (id)
            on delete cascade
)
    engine = InnoDB
    charset = utf8;

create table disease_concept_mapping
(
    id            bigint auto_increment
        primary key,
    disease_type  varchar(128) not null,
    concept_index int          not null,
    weight        double       not null default 1,
    create_time   datetime default CURRENT_TIMESTAMP null,
    update_time   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uq_disease_concept
        unique (disease_type, concept_index),
    constraint fk_disease_concept_dictionary
        foreign key (concept_index) references concept_dictionary (concept_index)
)
    engine = InnoDB
    charset = utf8;

create definer = root@localhost trigger BeforeUserUpdate_PreventOpenIDChange
    before update
    on user
    for each row
begin
    -- missing source code
end;

create table user_chat_session
(
    id                 bigint auto_increment
        primary key,
    user_id            bigint unsigned                    not null,
    chat_id            varchar(64) charset utf8           not null,
    title              varchar(128) charset utf8          null,
    created_at         datetime default CURRENT_TIMESTAMP null,
    updated_at         datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    last_activity_time datetime default CURRENT_TIMESTAMP null,
    constraint uq_user_chat_session_user_chat
        unique (user_id, chat_id),
    constraint user_chat_session_user_id_fk
        foreign key (user_id) references user (id)
)
    engine = InnoDB;

-- migration for existing databases (execute manually if table already exists)
-- ALTER TABLE user_chat_session ADD COLUMN title VARCHAR(128) CHARSET utf8 NULL AFTER chat_id;
-- ALTER TABLE user_chat_session ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP AFTER created_at;
-- ALTER TABLE user_chat_session ADD UNIQUE KEY uq_user_chat_session_user_chat (user_id, chat_id);
