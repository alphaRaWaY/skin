create table signon
(
    username   varchar(20)                         not null,
    password   varchar(255)                        not null,
    userid     int auto_increment
        primary key,
    created_at timestamp default CURRENT_TIMESTAMP null,
    updated_at timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    role       varchar(20)                         not null,
    email      varchar(255)                        not null,
    constraint signon_pk
        unique (email)
)
    row_format = DYNAMIC;

create table account
(
    userid     int          not null,
    firstname  varchar(255) not null,
    lastname   varchar(255) not null,
    addr1      varchar(255) not null,
    addr2      varchar(255) null,
    city       varchar(255) not null,
    state      varchar(255) not null,
    zip        varchar(255) not null,
    country    varchar(255) not null,
    phone      varchar(255) not null,
    updated_at timestamp    null on update CURRENT_TIMESTAMP,
    avatar     varchar(255) null,
    constraint account_signon_userid_fk
        foreign key (userid) references signon (userid)
            on delete cascade
)
    row_format = DYNAMIC;

create table password_reset_tokens
(
    id          bigint auto_increment
        primary key,
    email       varchar(255)                        not null,
    token       varchar(255)                        not null,
    expire_time datetime                            not null,
    created_at  timestamp default CURRENT_TIMESTAMP null
);



create table bannerdata
(
    favcategory varchar(80)  not null
        primary key,
    bannername  varchar(255) null
)
    charset = utf8mb4
    row_format = DYNAMIC;

create table cart
(
    itemid   varchar(255) not null,
    username varchar(255) null,
    quantity int          null
);

create table category
(
    catid int  not null
        primary key,
    catname  varchar(80)  not null,
    descn varchar(255) null
)
    charset = utf8mb4
    row_format = DYNAMIC;


create table language
(
    languageID int auto_increment
        primary key,
    languages  varchar(255) null
);

create table lineitem
(
    orderid   int            not null,
    linenum   int            not null,
    itemid    varchar(10)    not null,
    quantity  int            not null,
    unitprice decimal(10, 2) not null,
    primary key (orderid, linenum)
)
    charset = utf8mb4
    row_format = DYNAMIC;

create table log
(
    logID    int auto_increment
        primary key,
    username varchar(255) null,
    content  varchar(255) null
);

create table orders
(
    orderid         int(255) auto_increment
        primary key,
    userid          varchar(80)    not null,
    orderdate       date           not null,
    shipaddr1       varchar(80)    not null,
    shipaddr2       varchar(80)    null,
    shipcity        varchar(80)    not null,
    shipstate       varchar(80)    not null,
    shipzip         varchar(20)    not null,
    shipcountry     varchar(20)    not null,
    billaddr1       varchar(80)    not null,
    billaddr2       varchar(80)    null,
    billcity        varchar(80)    not null,
    billstate       varchar(80)    not null,
    billzip         varchar(20)    not null,
    billcountry     varchar(20)    not null,
    courier         varchar(80)    not null,
    totalprice      decimal(10, 2) not null,
    billtofirstname varchar(80)    not null,
    billtolastname  varchar(80)    not null,
    shiptofirstname varchar(80)    not null,
    shiptolastname  varchar(80)    not null,
    creditcard      varchar(80)    not null,
    exprdate        varchar(7)     not null,
    cardtype        varchar(80)    not null,
    locale          varchar(80)    not null,
    status          varchar(2)     null
)
    charset = utf8mb4
    row_format = DYNAMIC;

create table orderstatus
(
    orderid   int        not null,
    linenum   int        not null,
    timestamp date       not null,
    status    varchar(2) not null,
    primary key (orderid, linenum)
)
    charset = utf8mb4
    row_format = DYNAMIC;

create table product
(
    productid int  not null
        primary key,
    category  int  not null,
    productname      varchar(80)  not null,
    descn     varchar(255) null,
    constraint fk_product_1
        foreign key (category) references category (catid)
            on update cascade on delete cascade
)
    charset = utf8mb4
    row_format = DYNAMIC;

create table item
(
    itemid    int    not null
        primary key,
    productid int    not null,
    itemname  varchar(80)    not null,
    listprice decimal(10, 2) null,
    unitcost  decimal(10, 2) null,
    number    int            not null ,
    supplier  int         null,
    status    varchar(2)     null,
    attr1     varchar(80)    null,
    attr2     varchar(80)    null,
    attr3     varchar(80)    null,
    attr4     varchar(80)    null,
    attr5     varchar(80)    null,
    constraint fk_item_1
        foreign key (productid) references product (productid)
            on update cascade on delete cascade
)
    charset = utf8mb4
    row_format = DYNAMIC;

create index fk_item_2
    on item (supplier);

create index itemProd
    on item (productid);

create index productCat
    on product (category);

create index productName
    on product (productname);

create table profile
(
    userid      varchar(80) not null
        primary key,
    langpref    varchar(80) not null,
    favcategory varchar(30) null,
    mylistopt   int         null,
    banneropt   int         null
)
    charset = utf8mb4
    row_format = DYNAMIC;

create table sequence
(
    name   varchar(30) not null
        primary key,
    nextid int         not null
)
    charset = utf8mb4
    row_format = DYNAMIC;

create table shoppingcart
(
    userid   int         not null,
    QUANTITY int         not null,
    itemid   varchar(20) not null
);




create table supplier
(
    suppid int         not null
        primary key,
    name   varchar(80) null,
    status varchar(2)  not null,
    addr1  varchar(80) null,
    addr2  varchar(80) null,
    city   varchar(80) null,
    state  varchar(80) null,
    zip    varchar(5)  null,
    phone  varchar(80) null
)
    charset = utf8mb4
    row_format = DYNAMIC;

