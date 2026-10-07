-- gipr 地理标志产品保护与专用标志使用核准监管 -- schema (chen-029)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_029

CREATE TABLE IF NOT EXISTS t_gipr_archive_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '历史用标卷宗转档流水批次码',
  row_no int DEFAULT NULL COMMENT '原册行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的授权册代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行沉档卷数',
  chk_num decimal(12,2) DEFAULT NULL COMMENT '本行核对齐整数',
  put_kind varchar(32) DEFAULT NULL COMMENT '来路（县局自录移交／市里统交两条来路）',
  status int DEFAULT NULL COMMENT '行落地情形 0未勾 1已收下 2已退回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史用标卷宗转档核收行';

CREATE TABLE IF NOT EXISTS t_gipr_case_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '专用标志使用核准办件的件次',
  stage int DEFAULT NULL COMMENT '当前关次 0..3（申请受理/材料初审/标志核准/备案发标）',
  status int DEFAULT NULL COMMENT '单子走到哪一关 0未起 1在办 2已封住',
  content varchar(255) DEFAULT NULL COMMENT '一关一记',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次挪关动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专用标志使用核准办件流转单';

CREATE TABLE IF NOT EXISTS t_gipr_cert_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '专用标志使用授权签批单',
  holder_no varchar(64) DEFAULT NULL COMMENT '用标企业授权册代号（授权钉到哪一家）',
  prod_no varchar(64) DEFAULT NULL COMMENT '名录产品代号（授权钉到哪一批名录产品）',
  rule_code varchar(64) DEFAULT NULL COMMENT '点齐当场认定的那一道换算贴线',
  grant_id bigint DEFAULT NULL COMMENT '点齐那一刻由本单带出的授权行（只许本单回写）',
  node_no int DEFAULT NULL COMMENT '当前所在关 0..2（县级初核/市级核认/省局核准），一律后端当场数',
  sign_mode int DEFAULT NULL COMMENT '本关算齐办法 0先到作准 1两名须同判（关策当场数，报文自称不认）',
  need_count int DEFAULT NULL COMMENT '本关应签人数 1..2（定员量程，关策当场数；写0/9一笔不合进不了库）',
  sign_count int DEFAULT NULL COMMENT '本关本轮已到人数（当场从票行数，不靠手填）',
  veto_opinion varchar(500) DEFAULT NULL COMMENT '止住那一刻写回本行的不可原话（屏上列得出来；缓过来后清空）',
  status int DEFAULT NULL COMMENT '签批情形 0在签 1已点齐封住 2已止住（不可）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_bill_grant (grant_id),
  KEY idx_cert_bill_holder_status (holder_no, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专用标志使用授权签批单';

-- 签批票：一关一票一人；压回重签时上一轮票一并抹掉（物理删除、从头累计，旧票混新票即缺陷），
-- 压回动作本身在单据备注留痕
CREATE TABLE IF NOT EXISTS t_gipr_cert_vote (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint NOT NULL COMMENT '落在哪一张签批单',
  node_no int NOT NULL COMMENT '落在哪一关 0..2（按签人关籍落，后到沉底票沉在本关行面）',
  voter_no varchar(64) NOT NULL COMMENT '落笔签人代号',
  vote_kind int NOT NULL COMMENT '票形 1可 2不可（不可即止住）',
  opinion varchar(500) DEFAULT NULL COMMENT '落笔的话（止住时写回本关，屏上列得出来）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_vote_node_voter (bill_id, node_no, voter_no),
  KEY idx_cert_vote_bill_node (bill_id, node_no, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='签批单各关落笔票';

-- 关籍座次：每关定员由后端关策当场排（县1/市2/省2），落笔只认在籍签人；
-- 名单长一寸短一寸都不入库（应签人数写0/9即人数栏不合），不留手填段次的口子
CREATE TABLE IF NOT EXISTS t_gipr_cert_signer (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint NOT NULL COMMENT '落在哪一张签批单',
  node_no int NOT NULL COMMENT '落在哪一关 0..2',
  seat_no int NOT NULL COMMENT '座次 1..2（定员由关策当场数）',
  voter_no varchar(64) NOT NULL COMMENT '在籍签人代号',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_signer_seat (bill_id, node_no, seat_no),
  KEY idx_cert_signer_voter (bill_id, node_no, voter_no, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='签批单各关定员座次（关籍）';

-- 授权行：只能由签批单点齐那一刻带出，无另起/徒手造证入口
CREATE TABLE IF NOT EXISTS t_gipr_cert_grant (
  id bigint NOT NULL COMMENT '主键',
  grant_no varchar(64) NOT NULL COMMENT '证面编号：核发当日那一个数（当场按当日顺序取，邻张拿来不认）',
  bill_id bigint NOT NULL COMMENT '由哪一张签批单带出（一单一授）',
  holder_no varchar(64) NOT NULL COMMENT '用标企业代号（点齐当场钉死）',
  prod_no varchar(64) NOT NULL COMMENT '名录产品代号（点齐当场钉死）',
  rule_code varchar(64) DEFAULT NULL COMMENT '当时认定的那一道换算贴线（改线后重算判不符）',
  finish_node int NOT NULL COMMENT '走完那一刻的关次（当场取自单据，字字相等）',
  issue_date date NOT NULL COMMENT '核发当日',
  status int DEFAULT NULL COMMENT '授权情形 0在册 1已撤',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cert_grant_no (grant_no),
  UNIQUE KEY uk_cert_grant_bill (bill_id),
  KEY idx_cert_grant_holder (holder_no, prod_no, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专用标志使用授权记录（签批单点齐带出）';

CREATE TABLE IF NOT EXISTS t_gipr_check_item (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '年度核查提醒条目代号',
  due_at datetime DEFAULT NULL COMMENT '该发提醒那一一日（落库为 datetime）',
  amount decimal(12,2) DEFAULT NULL COMMENT '至多许提前几日',
  content varchar(255) DEFAULT NULL COMMENT '事由与所对企业的记要',
  status int DEFAULT NULL COMMENT '条目情形 0搁着 1已发出 2发不成',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年度核查与续展提醒条目';

CREATE TABLE IF NOT EXISTS t_gipr_holder_book (
  id bigint NOT NULL COMMENT '主键',
  holder_no varchar(64) DEFAULT NULL COMMENT '用标企业授权册代号',
  grant_id bigint DEFAULT NULL COMMENT '由哪一条授权改口（点齐同回钉入；年底凭它与授权行回查对齐）',
  prod_id int DEFAULT NULL COMMENT '挂在哪一个名录产品名下',
  prod_no varchar(64) DEFAULT NULL COMMENT '名录产品代号（冗余自名录底册）',
  src_kind varchar(32) DEFAULT NULL COMMENT '来路（县局代收／市里补报／企业直送三条来路）',
  area_num decimal(12,2) DEFAULT NULL COMMENT '在册经营面积数',
  content varchar(255) DEFAULT NULL COMMENT '随件交来的企业名号与主打品类记要',
  status int DEFAULT NULL COMMENT '册面情形 0新入册 1已核齐 2缺项',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专用标志使用企业授权底册';

CREATE TABLE IF NOT EXISTS t_gipr_product_book (
  id bigint NOT NULL COMMENT '主键',
  prod_no varchar(64) DEFAULT NULL COMMENT '地标产品名录代号',
  prod_name varchar(128) DEFAULT NULL COMMENT '名录全称',
  prod_kind varchar(32) DEFAULT NULL COMMENT '层类别（甲层种植类·乙层水产类·丙层初加工类）',
  road_name varchar(255) DEFAULT NULL COMMENT '产地范围详址（澜原省—渠州市—浚安县—镇—片区）',
  src_kind varchar(32) DEFAULT NULL COMMENT '来路（国家局核准公告下发／省里增补／本局自录）',
  status int DEFAULT NULL COMMENT '名录情形 0在册 1已移出',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地理标志产品名录底册';

CREATE TABLE IF NOT EXISTS t_gipr_term_book (
  id bigint NOT NULL COMMENT '主键',
  term_no varchar(64) DEFAULT NULL COMMENT '名录词条代号（由系统排上，不留手填的格子）',
  term_name varchar(128) DEFAULT NULL COMMENT '词条名目（汉文正名／外文对照／拼音缩写）',
  term_lang varchar(32) DEFAULT NULL COMMENT '本行语种（汉／外／拼 三式各一行）',
  term_text varchar(255) DEFAULT NULL COMMENT '词条文面',
  content varchar(255) DEFAULT NULL COMMENT '核验串（随版次走）',
  term_kind int DEFAULT NULL COMMENT '词条取用三式 0只出汉文 1只出对照 2按层回落',
  lang_num int DEFAULT NULL COMMENT '一屏并列展示的语种条数 1..3（量程外的数视同没填）',
  level_kind varchar(32) DEFAULT NULL COMMENT '定档层级（省定／市定／县定／系统兜底）',
  parent_no varchar(64) DEFAULT NULL COMMENT '继承自哪一条',
  prod_id int DEFAULT NULL COMMENT '挂在哪一个名录产品名下',
  prod_no varchar(64) DEFAULT NULL COMMENT '适用的名录产品代号（冗余自名录底册）',
  prod_name varchar(128) DEFAULT NULL COMMENT '名录全称（冗余一份，以名录底册为准回写）',
  ver_no int DEFAULT NULL COMMENT '改动次序（数值大的先说话）',
  crt_num int DEFAULT NULL COMMENT '本项应收录条数',
  hit_num int DEFAULT NULL COMMENT '已对齐条数',
  lack_num int DEFAULT NULL COMMENT '还差几条（轧出来，不留填数的空位）',
  eff_start datetime DEFAULT NULL COMMENT '启用那一一日',
  eff_end datetime DEFAULT NULL COMMENT '交班那一一日(不含)',
  status int DEFAULT NULL COMMENT '行的情形 0在位 1已撤下',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='名录对外公示多语言词条册';

CREATE TABLE IF NOT EXISTS t_gipr_yield_rule (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '换算贴线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  obj_kind varchar(32) DEFAULT NULL COMMENT '这条线管种植类、水产类还是初加工类',
  th1_max decimal(12,3) DEFAULT NULL COMMENT '关注限值',
  th2_max decimal(12,3) DEFAULT NULL COMMENT '告警限值',
  th3_max decimal(12,3) DEFAULT NULL COMMENT '封顶限值',
  unit_kind varchar(32) DEFAULT NULL COMMENT '计量单位口径（每亩公斤／每公顷吨 两式）',
  eff_start datetime DEFAULT NULL COMMENT '启用那一一日',
  eff_end datetime DEFAULT NULL COMMENT '交班那一一日(不含)',
  priority int DEFAULT NULL COMMENT '交班次序(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0在位 1已撤下',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='核定产量与特色指标换算贴线档';

-- 初始档案数据（id=1 启用 / id=2 停用）
-- 名录产品预置两条：prod id=0 在册、id=1 已移出，供「挂靠企业是否仍在册」与
-- 「已移出那一段不再收新行」两类业务判定取用。
-- 层类别（甲层/乙层）与产地范围按名录登记口径给值。
INSERT IGNORE INTO t_gipr_product_book (id, prod_no, prod_name, prod_kind, road_name, src_kind, status, del_flag, create_by, create_time)
VALUES (0, 'GP00', '澜原省渠州市浚安县浚浦蟹（甲层水产类·在册）', '甲层', '澜原省—渠州市—浚安县—北浚镇—浚浦水域', '国家局核准公告下发', 0, 0, 'seed', NOW()),
       (1, 'GP01', '南浚镇西片沙梨·已移出的旧名录（已移出）', '乙层', '澜原省—渠州市—浚安县—南浚镇—西片坡地', '省里增补', 1, 0, 'seed', NOW());

